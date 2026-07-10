package com.spring.dubbyserver.domain.billing;

import com.spring.dubbyserver.global.config.DubbyProperties;
import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * RevenueCat 웹훅 (스펙 §5.8).
 * 멱등+원자: revenuecat_events(event_id UNIQUE) insert와 entitlement 갱신을 같은 트랜잭션으로.
 * 처리 중 예외 → 500 (트랜잭션 롤백, RC 재시도 위임 — 유실 경로 차단).
 * 순서 역전 가드: event_timestamp_ms 기반 last-write-wins.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class RevenueCatWebhookController {

    private final JdbcClient jdbc;
    private final DubbyProperties properties;
    private final ObjectMapper objectMapper;

    @PostMapping("/revenuecat")
    @Transactional
    public Map<String, Object> webhook(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody Map<String, Object> payload) {
        String expected = "Bearer " + properties.revenuecat().webhookSecret();
        if (properties.revenuecat().webhookSecret() == null
                || properties.revenuecat().webhookSecret().isBlank()
                || !expected.equals(authorization)) {
            throw new DubbyException(ErrorCode.BILLING_WEBHOOK_UNAUTHORIZED);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) payload.get("event");
        if (event == null) {
            return Map.of("processed", false, "reason", "no event");
        }

        String eventId = str(event.get("id"));
        String type = str(event.get("type"));
        String appUserId = str(event.get("app_user_id"));
        Long timestampMs = num(event.get("event_timestamp_ms"));

        // 멱등: 이미 처리된 event_id면 즉시 200
        int inserted = jdbc.sql("""
                INSERT INTO revenuecat_events (event_id, event_type, app_user_id, event_timestamp_ms, payload)
                VALUES (:eventId, :type, :appUserId, :ts, CAST(:payload AS jsonb))
                ON CONFLICT (event_id) DO NOTHING
                """)
                .param("eventId", eventId)
                .param("type", type)
                .param("appUserId", appUserId)
                .param("ts", timestampMs)
                .param("payload", objectMapper.writeValueAsString(payload))
                .update();
        if (inserted == 0) {
            return Map.of("processed", true, "duplicate", true);
        }

        UUID userId = parseUser(appUserId);
        if (userId == null || type == null) {
            log.warn("RC 웹훅: 매핑 불가 app_user_id={} type={}", appUserId, type);
            return Map.of("processed", true, "skipped", true);
        }

        process(userId, type, event, timestampMs);
        return Map.of("processed", true);
    }

    private void process(UUID userId, String type, Map<String, Object> event, Long timestampMs) {
        String entitlementId = properties.billing().entitlementId();
        String coffeeProduct = properties.billing().products().get("coffee");
        String productId = str(event.get("product_id"));
        Long expirationMs = num(event.get("expiration_at_ms"));
        String environment = str(event.getOrDefault("environment", "PRODUCTION"));

        switch (type) {
            case "INITIAL_PURCHASE", "RENEWAL", "UNCANCELLATION", "PRODUCT_CHANGE" -> {
                if (hasEntitlement(event, entitlementId)) {
                    upsertEntitlement(userId, entitlementId, "ACTIVE", true, productId,
                            environment, expirationMs, timestampMs);
                }
            }
            case "CANCELLATION" -> // 자동갱신 해제 — 만료 전까지 SALARY 유지
                    updateWillRenew(userId, entitlementId, false, timestampMs);
            case "EXPIRATION" ->
                    upsertEntitlement(userId, entitlementId, "EXPIRED", false, productId,
                            environment, expirationMs, timestampMs);
            case "BILLING_ISSUE" ->
                    updateStatusOnly(userId, entitlementId, "BILLING_ISSUE", timestampMs);
            case "NON_RENEWING_PURCHASE" -> {
                if (coffeeProduct != null && coffeeProduct.equals(productId)) {
                    insertCoffee(userId, productId, str(event.get("transaction_id")));
                }
            }
            default -> log.info("RC 웹훅: 미처리 타입 {} (기록만)", type);
        }
    }

    private boolean hasEntitlement(Map<String, Object> event, String entitlementId) {
        Object ids = event.get("entitlement_ids");
        if (ids instanceof List<?> list) {
            return list.stream().map(String::valueOf).anyMatch(entitlementId::equals);
        }
        return entitlementId.equals(str(event.get("entitlement_id")));
    }

    private void upsertEntitlement(UUID userId, String entitlement, String status, boolean willRenew,
                                   String productId, String environment, Long expirationMs, Long timestampMs) {
        jdbc.sql("""
                INSERT INTO purchase_entitlements
                    (user_id, entitlement, status, product_id, environment, will_renew,
                     purchased_at, expires_at, last_event_at, updated_at)
                VALUES (:userId, :entitlement, :status, :productId, :environment, :willRenew,
                        now(), to_timestamp(:expMs / 1000.0), to_timestamp(:tsMs / 1000.0), now())
                ON CONFLICT (user_id, entitlement) DO UPDATE SET
                    status = :status, product_id = :productId, environment = :environment,
                    will_renew = :willRenew, expires_at = to_timestamp(:expMs / 1000.0),
                    last_event_at = to_timestamp(:tsMs / 1000.0), updated_at = now()
                WHERE purchase_entitlements.last_event_at IS NULL
                   OR purchase_entitlements.last_event_at <= to_timestamp(:tsMs / 1000.0)
                """)
                .param("userId", userId)
                .param("entitlement", entitlement)
                .param("status", status)
                .param("productId", productId)
                .param("environment", environment)
                .param("willRenew", willRenew)
                .param("expMs", expirationMs != null ? expirationMs : 0L)
                .param("tsMs", timestampMs != null ? timestampMs : 0L)
                .update();
    }

    private void updateWillRenew(UUID userId, String entitlement, boolean willRenew, Long timestampMs) {
        jdbc.sql("""
                UPDATE purchase_entitlements SET will_renew = :willRenew,
                       last_event_at = to_timestamp(:tsMs / 1000.0), updated_at = now()
                WHERE user_id = :userId AND entitlement = :entitlement
                  AND (last_event_at IS NULL OR last_event_at <= to_timestamp(:tsMs / 1000.0))
                """)
                .param("willRenew", willRenew).param("tsMs", timestampMs != null ? timestampMs : 0L)
                .param("userId", userId).param("entitlement", entitlement)
                .update();
    }

    private void updateStatusOnly(UUID userId, String entitlement, String status, Long timestampMs) {
        jdbc.sql("""
                UPDATE purchase_entitlements SET status = :status,
                       last_event_at = to_timestamp(:tsMs / 1000.0), updated_at = now()
                WHERE user_id = :userId AND entitlement = :entitlement
                  AND (last_event_at IS NULL OR last_event_at <= to_timestamp(:tsMs / 1000.0))
                """)
                .param("status", status).param("tsMs", timestampMs != null ? timestampMs : 0L)
                .param("userId", userId).param("entitlement", entitlement)
                .update();
    }

    private void insertCoffee(UUID userId, String productId, String transactionId) {
        // rc_transaction_id UNIQUE — 재전송 멱등
        jdbc.sql("""
                INSERT INTO one_time_purchases (user_id, product_id, rc_transaction_id, effect_expires_at)
                VALUES (:userId, :productId, :txId, now() + CAST(:duration AS interval))
                ON CONFLICT (rc_transaction_id) DO NOTHING
                """)
                .param("userId", userId)
                .param("productId", productId)
                .param("txId", transactionId != null ? transactionId : UUID.randomUUID().toString())
                .param("duration", properties.billing().coffeeEffectDuration().toSeconds() + " seconds")
                .update();
    }

    private static UUID parseUser(String appUserId) {
        try {
            return UUID.fromString(appUserId);
        } catch (Exception e) {
            return null; // RC 익명 ID 등 — 매핑 불가
        }
    }

    private static String str(Object o) {
        return o != null ? String.valueOf(o) : null;
    }

    private static Long num(Object o) {
        return o instanceof Number n ? n.longValue() : null;
    }
}
