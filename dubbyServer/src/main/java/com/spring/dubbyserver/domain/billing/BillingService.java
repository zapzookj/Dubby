package com.spring.dubbyserver.domain.billing;

import com.spring.dubbyserver.global.config.DubbyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 등급 판정 단일 지점 (스펙 §12) — SALARY(구독 활성) > SUPPORTER(커피 24h) > FREE.
 * 기능 제한 판정은 항상 서버(이 클래스), 클라 usePremium()은 UI 게이팅만.
 */
@Service
@RequiredArgsConstructor
public class BillingService {

    private final JdbcClient jdbc;
    private final DubbyProperties properties;

    public enum Tier { FREE, SUPPORTER, SALARY }

    public record BillingStatus(Tier tier, Instant expiresAt, boolean willRenew, Instant coffeeActiveUntil) {}

    public Tier resolveTier(UUID userId) {
        return status(userId).tier();
    }

    public boolean isPremium(UUID userId) {
        return resolveTier(userId) == Tier.SALARY;
    }

    public BillingStatus status(UUID userId) {
        var salary = jdbc.sql("""
                SELECT expires_at, will_renew FROM purchase_entitlements
                WHERE user_id = :userId AND entitlement = :entitlement
                  AND status = 'ACTIVE' AND (expires_at IS NULL OR expires_at > now())
                """)
                .param("userId", userId)
                .param("entitlement", properties.billing().entitlementId())
                .query((rs, i) -> Map.of(
                        "expiresAt", Optional.ofNullable(rs.getTimestamp("expires_at"))
                                .map(java.sql.Timestamp::toInstant),
                        "willRenew", rs.getBoolean("will_renew")))
                .optional();

        Optional<Instant> coffee = jdbc.sql("""
                SELECT MAX(effect_expires_at) FROM one_time_purchases
                WHERE user_id = :userId AND effect_expires_at > now()
                """)
                .param("userId", userId)
                .query(java.sql.Timestamp.class)
                .optional()
                .map(java.sql.Timestamp::toInstant);

        if (salary.isPresent()) {
            @SuppressWarnings("unchecked")
            Optional<Instant> expiresAt = (Optional<Instant>) salary.get().get("expiresAt");
            return new BillingStatus(Tier.SALARY, expiresAt.orElse(null),
                    (Boolean) salary.get().get("willRenew"), coffee.orElse(null));
        }
        if (coffee.isPresent()) {
            return new BillingStatus(Tier.SUPPORTER, null, false, coffee.get());
        }
        return new BillingStatus(Tier.FREE, null, false, null);
    }

    /** tier → 일일 채팅 한도 (yml) */
    public int chatDailyLimit(Tier tier) {
        DubbyProperties.Chat.DailyLimit limits = properties.chat().dailyLimit();
        return switch (tier) {
            case SALARY -> limits.salary();
            case SUPPORTER -> limits.supporter();
            case FREE -> limits.free();
        };
    }

    /** tier → 일기 슬롯 한도 (yml) */
    public int diarySlotLimit(Tier tier) {
        return tier == Tier.SALARY
                ? properties.diary().slotLimit().salary()
                : properties.diary().slotLimit().free();
    }
}
