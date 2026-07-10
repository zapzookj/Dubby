package com.spring.dubbyserver.domain.push;

import com.spring.dubbyserver.infra.expo.ExpoPushClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Expo 영수증 확인 배치 — DeviceNotRegistered 토큰 무효화, 48h 지난 SENT는 DELIVERED 마감 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushReceiptScheduler {

    private final JdbcClient jdbc;
    private final ExpoPushClient expo;

    private record Sent(long id, String ticketId, java.util.UUID userId) {}

    @Scheduled(fixedDelayString = "${dubby.push.receipt-interval}")
    public void checkReceipts() {
        List<Sent> sents = jdbc.sql("""
                SELECT id, expo_ticket_id, user_id FROM push_send_logs
                WHERE status = 'SENT' AND expo_ticket_id IS NOT NULL
                  AND sent_at < now() - interval '15 minutes'
                  AND receipt_checked_at IS NULL
                LIMIT 1000
                """)
                .query((rs, i) -> new Sent(rs.getLong("id"), rs.getString("expo_ticket_id"),
                        rs.getObject("user_id", java.util.UUID.class)))
                .list();

        if (!sents.isEmpty()) {
            Map<String, ExpoPushClient.Receipt> receipts =
                    expo.getReceipts(sents.stream().map(Sent::ticketId).toList());
            for (Sent sent : sents) {
                ExpoPushClient.Receipt receipt = receipts.get(sent.ticketId());
                if (receipt == null) continue; // 아직 미준비 — 다음 주기 재확인
                if ("ok".equals(receipt.status())) {
                    jdbc.sql("UPDATE push_send_logs SET status = 'DELIVERED', receipt_checked_at = now() WHERE id = :id")
                            .param("id", sent.id()).update();
                } else {
                    jdbc.sql("UPDATE push_send_logs SET status = 'RECEIPT_ERROR', error_code = :error, receipt_checked_at = now() WHERE id = :id")
                            .param("error", receipt.error()).param("id", sent.id()).update();
                    if ("DeviceNotRegistered".equals(receipt.error())) {
                        jdbc.sql("""
                                UPDATE push_tokens SET is_active = FALSE
                                WHERE user_id = :userId AND is_active
                                """)
                                .param("userId", sent.userId()).update();
                    }
                }
            }
        }

        // Expo 영수증 보존 기한(약 24h~) 지난 SENT는 DELIVERED 간주 마감
        int closed = jdbc.sql("""
                UPDATE push_send_logs SET status = 'DELIVERED', receipt_checked_at = now()
                WHERE status = 'SENT' AND sent_at < now() - interval '48 hours'
                """).update();
        if (closed > 0) {
            log.info("영수증 마감 처리: {}건", closed);
        }
    }
}
