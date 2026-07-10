package com.spring.dubbyserver.domain.metrics;

import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 운영 지표 요약 — X-Admin-Key 헤더(env ADMIN_KEY) 인증.
 * ADMIN_KEY 미설정 시 전면 차단 (안전 기본값).
 */
@RestController
@RequestMapping("/api/v1/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final JdbcClient jdbc;

    @GetMapping("/daily")
    public Map<String, Object> daily(@RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        String expected = System.getenv("ADMIN_KEY");
        if (expected == null || expected.isBlank() || !expected.equals(adminKey)) {
            throw new DubbyException(ErrorCode.AUTH_TOKEN_INVALID, "admin key required");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dau", single("SELECT COUNT(*) FROM user_activity_daily WHERE activity_date = CURRENT_DATE"));
        body.put("newUsers", single("SELECT COUNT(*) FROM users WHERE created_at >= CURRENT_DATE"));
        body.put("chatMessagesToday", single(
                "SELECT COUNT(*) FROM chat_messages WHERE role = 'USER' AND created_at >= CURRENT_DATE"));
        body.put("chatLimitHits", single(
                "SELECT COUNT(*) FROM app_events WHERE event_type = 'CHAT_LIMIT_HIT' AND occurred_at >= CURRENT_DATE"));
        body.put("llmTokensToday", single(
                "SELECT COALESCE(total_tokens, 0) FROM global_llm_usage WHERE usage_date = CURRENT_DATE"));
        body.put("llmValidationFailures", single(
                "SELECT COUNT(*) FROM llm_usage_log WHERE validation_failed AND created_at >= CURRENT_DATE"));
        body.put("pushSentToday", single(
                "SELECT COUNT(*) FROM push_send_logs WHERE status IN ('SENT','DELIVERED') AND created_at >= CURRENT_DATE"));
        body.put("pushOpenedToday", single(
                "SELECT COUNT(*) FROM push_send_logs WHERE opened_at >= CURRENT_DATE"));
        body.put("diarySavedToday", single(
                "SELECT COUNT(*) FROM app_events WHERE event_type = 'DIARY_SAVED' AND occurred_at >= CURRENT_DATE"));
        body.put("taskReactionsToday", single(
                "SELECT COUNT(*) FROM app_events WHERE event_type = 'TASK_REACTION' AND occurred_at >= CURRENT_DATE"));
        body.put("salarySubscribers", single(
                "SELECT COUNT(*) FROM purchase_entitlements WHERE status = 'ACTIVE' AND (expires_at IS NULL OR expires_at > now())"));
        return body;
    }

    private long single(String sql) {
        return jdbc.sql(sql).query(Long.class).optional().orElse(0L);
    }
}
