package com.spring.dubbyserver.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 튜닝 대상 상수 총람 (application.yml의 dubby.*).
 * 모든 수치성 상수는 코드 하드코딩 금지 — 반드시 이 클래스로만 접근한다.
 */
@ConfigurationProperties(prefix = "dubby")
public record DubbyProperties(
        Auth auth,
        User user,
        Task task,
        Chat chat,
        Diary diary,
        Push push,
        Billing billing,
        Llm llm,
        Revenuecat revenuecat
) {
    public record Auth(String jwtSecret, Duration accessTokenTtl) {}

    public record User(int nicknameMaxLength, Duration timezoneChangeMinInterval) {}

    public record Task(int dailyCount, int retryMax, int defaultCooldownDays) {}

    public record Chat(DailyLimit dailyLimit, int maxContentLength, int historyMaxMessages, int perUserConcurrency) {
        public record DailyLimit(int free, int supporter, int salary) {}
    }

    public record Diary(SlotLimit slotLimit, Duration candidateTtl, int dailyCandidateMax,
                        int candidateMinGapMessages, int rewriteDailyLimit) {
        public record SlotLimit(int free, int salary) {}
    }

    public record Push(int defaultDailyCount, int maxDailyCount, int defaultCooldownDays,
                       Duration dispatchInterval, Duration receiptInterval,
                       Map<String, Window> windows,
                       Map<Integer, List<String>> defaultSlotsByCount,
                       Window quiet) {
        public record Window(LocalTime start, LocalTime end) {}
    }

    public record Billing(String entitlementId, Map<String, String> products, Duration coffeeEffectDuration) {}

    public record Llm(String promptVersion, OpenRouter openrouter, Map<String, Route> routing,
                      Map<String, Integer> misreadLevelWeights, int inputTokenHardCap, Budget budget) {
        public record OpenRouter(String baseUrl, String apiKey, Duration connectTimeout, Duration readTimeout) {}
        public record Route(String primary, List<String> fallbacks, double temperature, int maxTokens) {}
        public record Budget(long globalDailyTokenLimit) {}
    }

    public record Revenuecat(String webhookSecret) {}
}
