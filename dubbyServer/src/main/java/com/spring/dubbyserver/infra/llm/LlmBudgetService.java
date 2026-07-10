package com.spring.dubbyserver.infra.llm;

import com.spring.dubbyserver.global.config.DubbyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

/** 전체 일일 토큰 캡 (운영자 파산 방지) — global_llm_usage 단일 로우, UTC 기준 */
@Service
@RequiredArgsConstructor
public class LlmBudgetService {

    private final JdbcClient jdbc;
    private final DubbyProperties properties;

    public boolean isGlobalBudgetExhausted() {
        long used = jdbc.sql("SELECT COALESCE(total_tokens, 0) FROM global_llm_usage WHERE usage_date = :date")
                .param("date", LocalDate.now(ZoneOffset.UTC))
                .query(Long.class).optional().orElse(0L);
        return used >= properties.llm().budget().globalDailyTokenLimit();
    }

    public void addUsage(int tokens, double costUsd) {
        jdbc.sql("""
                INSERT INTO global_llm_usage (usage_date, total_tokens, total_cost_usd)
                VALUES (:date, :tokens, :cost)
                ON CONFLICT (usage_date) DO UPDATE SET
                    total_tokens = global_llm_usage.total_tokens + :tokens,
                    total_cost_usd = global_llm_usage.total_cost_usd + :cost
                """)
                .param("date", LocalDate.now(ZoneOffset.UTC))
                .param("tokens", tokens)
                .param("cost", costUsd)
                .update();
    }
}
