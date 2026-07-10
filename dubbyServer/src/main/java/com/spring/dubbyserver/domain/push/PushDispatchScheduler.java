package com.spring.dubbyserver.domain.push;

import com.spring.dubbyserver.global.config.DubbyProperties;
import com.spring.dubbyserver.infra.expo.ExpoPushClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 푸시 발송 배치 — 스펙 §11.
 * 타임존별 크론 없이 단일 폴링 잡. 멱등 보장: UNIQUE(user_id, local_date, slot) + PENDING 선삽입.
 * 발송 대상 토큰: 유저당 최신 활성 토큰 1개 (슬롯당 1건 원칙).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushDispatchScheduler {

    private final JdbcClient jdbc;
    private final DubbyProperties properties;
    private final ExpoPushClient expo;

    private record Target(UUID userId, String timezone, String nickname, String locale,
                          int maxDailyCount, String token) {}

    private record Pending(long logId, UUID userId, String token, String title, String body, long templateId) {}

    @Scheduled(fixedDelayString = "${dubby.push.dispatch-interval}")
    public void dispatch() {
        List<Target> targets = jdbc.sql("""
                SELECT u.id AS user_id, u.timezone, u.nickname, u.locale, s.max_daily_count,
                       (SELECT t.expo_push_token FROM push_tokens t
                        WHERE t.user_id = u.id AND t.is_active
                        ORDER BY t.last_used_at DESC LIMIT 1) AS token
                FROM users u
                JOIN push_settings s ON s.user_id = u.id AND s.enabled
                WHERE u.status = 'ACTIVE'
                """)
                .query((rs, i) -> new Target(
                        rs.getObject("user_id", UUID.class), rs.getString("timezone"),
                        rs.getString("nickname"), rs.getString("locale"),
                        rs.getInt("max_daily_count"), rs.getString("token")))
                .list()
                .stream().filter(t -> t.token() != null).toList();

        List<Pending> pendings = new ArrayList<>();
        for (Target target : targets) {
            try {
                preparePending(target).ifPresent(pendings::add);
            } catch (Exception e) {
                log.warn("푸시 준비 실패 user={}: {}", target.userId(), e.getMessage());
            }
        }
        if (pendings.isEmpty()) return;

        // Expo 발송 (트랜잭션 밖) → 티켓 반영
        List<ExpoPushClient.PushMessage> messages = pendings.stream()
                .map(p -> new ExpoPushClient.PushMessage(p.token(), p.title(), p.body(),
                        Map.of("pushLogId", p.logId(), "deeplink", deeplinkOf(p))))
                .toList();
        List<ExpoPushClient.Ticket> tickets = expo.send(messages);
        for (int i = 0; i < pendings.size(); i++) {
            applyTicket(pendings.get(i), tickets.get(i));
        }
        log.info("푸시 발송: {}건", pendings.size());
    }

    /** 슬롯/quiet/지터 판정 → 템플릿 선정 → PENDING 선삽입 (ON CONFLICT 멱등) */
    private java.util.Optional<Pending> preparePending(Target target) {
        ZoneId zone = safeZone(target.timezone());
        LocalTime now = LocalTime.now(zone);
        LocalDate localDate = LocalDate.now(zone);

        // quiet 하드 가드 (22:00–08:00)
        DubbyProperties.Push.Window quiet = properties.push().quiet();
        if (!now.isBefore(quiet.start()) || now.isBefore(quiet.end())) {
            return java.util.Optional.empty();
        }

        // maxDailyCount에 해당하는 슬롯 중 현재 창 안인 것
        List<String> slots = properties.push().defaultSlotsByCount()
                .getOrDefault(target.maxDailyCount(), List.of("EVENING"));
        String currentSlot = null;
        DubbyProperties.Push.Window window = null;
        for (String slot : slots) {
            DubbyProperties.Push.Window w = properties.push().windows().get(slot.toLowerCase());
            if (w != null && !now.isBefore(w.start()) && now.isBefore(w.end())) {
                currentSlot = slot;
                window = w;
                break;
            }
        }
        if (currentSlot == null) return java.util.Optional.empty();

        // 지터: hash(userId, epochDay) % 창 길이(분) — 매일 다른 시각 발송
        long windowMinutes = Duration.between(window.start(), window.end()).toMinutes();
        long jitter = Math.floorMod(target.userId().hashCode() * 31L + localDate.toEpochDay(), windowMinutes);
        if (Duration.between(window.start(), now).toMinutes() < jitter) {
            return java.util.Optional.empty();
        }

        // 템플릿 선정: 30일 쿨다운(push_send_logs 원장), 같은 날 같은 category 제외, 시간창, 닉네임 필터
        var template = jdbc.sql("""
                SELECT t.id, t.content->>'title' AS title, t.content->>'body' AS body,
                       COALESCE(t.content->>'deeplink', 'dubby://home') AS deeplink
                FROM templates t
                WHERE t.type = 'PUSH' AND t.status = 'ACTIVE' AND t.locale = :locale
                  AND t.time_window IN ('ANY', :slot)
                  AND (t.requires_user_name = FALSE OR :hasNickname)
                  AND NOT EXISTS (
                      SELECT 1 FROM push_send_logs l
                      WHERE l.user_id = :userId AND l.template_id = t.id
                        AND l.local_date > (CAST(:localDate AS date) - t.cooldown_days))
                  AND t.category NOT IN (
                      SELECT t2.category FROM push_send_logs l2 JOIN templates t2 ON t2.id = l2.template_id
                      WHERE l2.user_id = :userId AND l2.local_date = :localDate)
                ORDER BY random() LIMIT 1
                """)
                .param("locale", target.locale())
                .param("slot", currentSlot)
                .param("hasNickname", target.nickname() != null)
                .param("userId", target.userId())
                .param("localDate", localDate)
                .query((rs, i) -> new String[]{
                        String.valueOf(rs.getLong("id")), rs.getString("title"),
                        rs.getString("body"), rs.getString("deeplink")})
                .optional();
        if (template.isEmpty()) return java.util.Optional.empty(); // 후보 없으면 스킵 (완화 없음 — 반복이 더 나쁨)

        String[] t = template.get();
        String nickname = target.nickname() != null ? target.nickname() : "사용자님";
        String title = t[1].replace("{nickname}", nickname);
        String body = t[2].replace("{nickname}", nickname);

        // PENDING 선삽입 — 발송 전 기록. 이미 이 슬롯에 행이 있으면(재시작/중복 실행) 스킵
        var logId = jdbc.sql("""
                INSERT INTO push_send_logs (user_id, template_id, slot, local_date, title, body, status)
                VALUES (:userId, :templateId, :slot, :localDate, :title, :body, 'PENDING')
                ON CONFLICT (user_id, local_date, slot) DO NOTHING
                RETURNING id
                """)
                .param("userId", target.userId())
                .param("templateId", Long.parseLong(t[0]))
                .param("slot", currentSlot)
                .param("localDate", localDate)
                .param("title", title)
                .param("body", body)
                .query(Long.class).optional();

        return logId.map(id -> new Pending(id, target.userId(), target.token(), title, body, Long.parseLong(t[0])));
    }

    private String deeplinkOf(Pending p) {
        return jdbc.sql("SELECT COALESCE(content->>'deeplink', 'dubby://home') FROM templates WHERE id = :id")
                .param("id", p.templateId()).query(String.class).optional().orElse("dubby://home");
    }

    private void applyTicket(Pending pending, ExpoPushClient.Ticket ticket) {
        if ("ok".equals(ticket.status())) {
            jdbc.sql("UPDATE push_send_logs SET status = 'SENT', expo_ticket_id = :ticket, sent_at = now() WHERE id = :id")
                    .param("ticket", ticket.id()).param("id", pending.logId()).update();
        } else {
            jdbc.sql("UPDATE push_send_logs SET status = 'TICKET_ERROR', error_code = :error, sent_at = now() WHERE id = :id")
                    .param("error", ticket.error()).param("id", pending.logId()).update();
            if ("DeviceNotRegistered".equals(ticket.error())) {
                deactivateToken(pending.token()); // 티켓 단계에서도 즉시 무효화
            }
        }
    }

    void deactivateToken(String token) {
        jdbc.sql("UPDATE push_tokens SET is_active = FALSE WHERE expo_push_token = :token")
                .param("token", token).update();
    }

    private static ZoneId safeZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("Asia/Seoul");
        }
    }
}
