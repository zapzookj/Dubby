package com.spring.dubbyserver.domain.home;

import com.spring.dubbyserver.domain.billing.BillingService;
import com.spring.dubbyserver.domain.home.dto.HomeResponse;
import com.spring.dubbyserver.domain.task.DailyTaskAssignmentRepository;
import com.spring.dubbyserver.domain.template.Template;
import com.spring.dubbyserver.domain.template.TemplateRepository;
import com.spring.dubbyserver.domain.user.User;
import com.spring.dubbyserver.domain.user.UserService;
import com.spring.dubbyserver.global.config.DubbyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** 홈 화면 1회 호출 조합 — 자체 엔티티 없음 (스펙 §5.2) */
@Service
@RequiredArgsConstructor
public class HomeService {

    private final UserService userService;
    private final TemplateRepository templateRepository;
    private final DailyTaskAssignmentRepository assignmentRepository;
    private final DubbyProperties properties;
    private final BillingService billingService;
    private final JdbcClient jdbc;

    private static final String[] DAY_MOODS = {"idle", "confident", "thinking", "happy"};

    @Transactional(readOnly = true)
    public HomeResponse getHome(UUID userId) {
        User user = userService.getActiveUser(userId);
        ZoneId zone = ZoneId.of(user.getTimezone());
        LocalDate localDate = LocalDate.now(zone);
        long seed = daySeed(userId, localDate);

        // 상태 문구: HOME_STATUS 템플릿 중 (userId, 날짜) 시드 고정 선택 — 같은 날 재진입 시 동일
        List<Template> statuses = templateRepository
                .findByTypeAndStatusAndLocaleOrderByIdAsc(Template.TemplateType.HOME_STATUS, "ACTIVE", user.getLocale());
        String statusLine = "근거 없는 자신감";
        String currentWork = "사용자를 돕는 척하기";
        if (!statuses.isEmpty()) {
            Template t = statuses.get((int) Math.floorMod(seed, statuses.size()));
            statusLine = t.contentString("statusLine");
            currentWork = t.contentString("currentWork");
        }

        // 정확도: 87~142% 시드 랜덤 (기획 — 개그 수치, 실제 성능 무관)
        String accuracy = (87 + Math.floorMod(seed >> 8, 56)) + "%";

        // 채팅 쿼터 (등급별 한도)
        BillingService.BillingStatus billing = billingService.status(userId);
        int limit = billingService.chatDailyLimit(billing.tier());
        int used = jdbc.sql("""
                        SELECT COALESCE(used_count, 0) FROM chat_daily_usage
                        WHERE user_id = :userId AND usage_date = :date
                        """)
                .param("userId", userId).param("date", localDate)
                .query(Integer.class).optional().orElse(0);
        int remaining = Math.max(0, limit - used);

        // 오늘의 업무 현황 (미배정이면 total=일일 개수, reacted=0)
        long reacted = assignmentRepository.countByUserIdAndAssignedDateAndReactionIsNotNull(userId, localDate);

        // 일기 현황 (P2 전까지 테이블 직조회 — 항상 0)
        long totalEntries = jdbc.sql("SELECT COUNT(*) FROM diary_entries WHERE user_id = :userId")
                .param("userId", userId).query(Long.class).single();
        long pendingCandidates = jdbc.sql("""
                        SELECT COUNT(*) FROM diary_candidates
                        WHERE user_id = :userId AND status = 'PENDING' AND expires_at > now()
                        """)
                .param("userId", userId).query(Long.class).single();

        String mood = resolveMood(seed, remaining, LocalTime.now(zone));

        return new HomeResponse(
                new HomeResponse.Derby(mood, statusLine, accuracy, currentWork),
                new HomeResponse.TodayTasks(localDate.toString(), properties.task().dailyCount(), (int) reacted),
                new HomeResponse.ChatQuota(billing.tier().name(), limit, used, remaining),
                new HomeResponse.Diary(totalEntries, pendingCandidates),
                new HomeResponse.Billing(billing.tier().name(),
                        billing.expiresAt() != null ? billing.expiresAt().toString() : null));
    }

    /** mood 우선순위: 과로(쿼터 소진) > 심야 취침 > 일자 시드 기본 무드 */
    private String resolveMood(long seed, int chatRemaining, LocalTime localTime) {
        if (chatRemaining == 0) return "collapsed";
        if (localTime.isAfter(LocalTime.of(22, 0)) || localTime.isBefore(LocalTime.of(6, 0))) return "sleeping";
        return DAY_MOODS[(int) Math.floorMod(seed >> 16, DAY_MOODS.length)];
    }

    private static long daySeed(UUID userId, LocalDate date) {
        long h = userId.getMostSignificantBits() ^ userId.getLeastSignificantBits();
        return h * 31 + date.toEpochDay();
    }
}
