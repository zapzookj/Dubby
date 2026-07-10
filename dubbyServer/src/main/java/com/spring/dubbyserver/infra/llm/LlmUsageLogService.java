package com.spring.dubbyserver.infra.llm;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmUsageLogService {

    private final JdbcClient jdbc;
    private final LlmBudgetService budgetService;

    @Builder
    public record Entry(UUID userId, LlmPurpose purpose, String model, String promptVersion,
                        Integer misreadLevel, int promptTokens, int completionTokens,
                        long latencyMs, String finishReason, String safetyCategory,
                        boolean validationFailed, boolean fallbackUsed) {}

    public void log(Entry e) {
        try {
            jdbc.sql("""
                    INSERT INTO llm_usage_log (user_id, purpose, model, prompt_version, misread_level,
                        prompt_tokens, completion_tokens, latency_ms, finish_reason, safety_category,
                        validation_failed, fallback_used)
                    VALUES (:userId, :purpose, :model, :promptVersion, :misreadLevel,
                        :promptTokens, :completionTokens, :latencyMs, :finishReason, :safetyCategory,
                        :validationFailed, :fallbackUsed)
                    """)
                    .param("userId", e.userId())
                    .param("purpose", e.purpose().name())
                    .param("model", e.model())
                    .param("promptVersion", e.promptVersion())
                    .param("misreadLevel", e.misreadLevel())
                    .param("promptTokens", e.promptTokens())
                    .param("completionTokens", e.completionTokens())
                    .param("latencyMs", (int) e.latencyMs())
                    .param("finishReason", e.finishReason())
                    .param("safetyCategory", e.safetyCategory())
                    .param("validationFailed", e.validationFailed())
                    .param("fallbackUsed", e.fallbackUsed())
                    .update();
            budgetService.addUsage(e.promptTokens() + e.completionTokens(), 0);
        } catch (Exception ex) {
            log.warn("llm_usage_log 기록 실패: {}", ex.getMessage());
        }
    }
}
