package com.spring.dubbyserver.domain.chat;

import com.spring.dubbyserver.global.config.DubbyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * 일일 채팅 쿼터 — 유저 로컬 날짜 기준 lazy 리셋 (배치 없음), 원자적 선차감 (스펙 §10).
 */
@Service
@RequiredArgsConstructor
public class ChatQuotaService {

    private final JdbcClient jdbc;
    private final DubbyProperties properties;

    /** tier별 일일 한도 (yml) */
    public int limitFor(String tier) {
        DubbyProperties.Chat.DailyLimit limits = properties.chat().dailyLimit();
        return switch (tier) {
            case "SALARY" -> limits.salary();
            case "SUPPORTER" -> limits.supporter();
            default -> limits.free();
        };
    }

    /** 원자적 선차감. 성공 시 차감 후 used_count, 한도 도달 시 empty */
    public Optional<Integer> tryConsume(UUID userId, LocalDate usageDate, int limit) {
        return jdbc.sql("""
                INSERT INTO chat_daily_usage (user_id, usage_date, used_count)
                VALUES (:userId, :usageDate, 1)
                ON CONFLICT (user_id, usage_date)
                DO UPDATE SET used_count = chat_daily_usage.used_count + 1, updated_at = now()
                WHERE chat_daily_usage.used_count < :limit
                RETURNING used_count
                """)
                .param("userId", userId)
                .param("usageDate", usageDate)
                .param("limit", limit)
                .query(Integer.class)
                .optional();
    }

    /** LLM 실패/SAFETY 대체 시 환불 */
    public void refund(UUID userId, LocalDate usageDate) {
        jdbc.sql("""
                UPDATE chat_daily_usage SET used_count = GREATEST(used_count - 1, 0), updated_at = now()
                WHERE user_id = :userId AND usage_date = :usageDate
                """)
                .param("userId", userId)
                .param("usageDate", usageDate)
                .update();
    }

    public int used(UUID userId, LocalDate usageDate) {
        return jdbc.sql("SELECT COALESCE(used_count, 0) FROM chat_daily_usage WHERE user_id = :userId AND usage_date = :date")
                .param("userId", userId)
                .param("date", usageDate)
                .query(Integer.class).optional().orElse(0);
    }
}
