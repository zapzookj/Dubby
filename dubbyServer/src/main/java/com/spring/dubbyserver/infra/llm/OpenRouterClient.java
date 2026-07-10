package com.spring.dubbyserver.infra.llm;

import com.spring.dubbyserver.global.config.DubbyProperties;
import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenRouter chat/completions 호출 (동기 RestClient).
 * - models 배열로 공급자 측 자동 폴백
 * - usage.include로 실제 토큰 사용량 수신
 * - 수동 재시도 1회 (네트워크/429/5xx), 서킷은 인메모리 카운터
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "dubby.llm.mock", havingValue = "false", matchIfMissing = true)
public class OpenRouterClient implements LlmClient {

    private final RestClient restClient;
    private final DubbyProperties.Llm llm;

    // 단순 서킷: 최근 실패 연속 카운트 (60초 내 5연속 실패 시 60초 차단)
    private volatile int consecutiveFailures = 0;
    private volatile long circuitOpenUntil = 0;

    public OpenRouterClient(DubbyProperties properties) {
        this.llm = properties.llm();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(llm.openrouter().connectTimeout());
        factory.setReadTimeout(llm.openrouter().readTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(llm.openrouter().baseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + llm.openrouter().apiKey())
                .defaultHeader("HTTP-Referer", "https://github.com/zapzookj/Dubby")
                .defaultHeader("X-Title", "Dubby")
                .build();
    }

    @Override
    public LlmResult complete(LlmPurpose purpose, List<Message> messages, boolean jsonMode) {
        if (System.currentTimeMillis() < circuitOpenUntil) {
            throw new DubbyException(ErrorCode.LLM_UPSTREAM_ERROR, "Circuit open");
        }
        try {
            LlmResult result = doComplete(purpose, messages, jsonMode, true);
            consecutiveFailures = 0;
            return result;
        } catch (DubbyException e) {
            if (++consecutiveFailures >= 5) {
                circuitOpenUntil = System.currentTimeMillis() + 60_000;
                consecutiveFailures = 0;
                log.warn("LLM 서킷 오픈 (60초)");
            }
            throw e;
        }
    }

    private LlmResult doComplete(LlmPurpose purpose, List<Message> messages, boolean jsonMode, boolean allowRetry) {
        DubbyProperties.Llm.Route route = llm.routing().get(purpose.routingKey());
        List<String> models = new ArrayList<>();
        models.add(route.primary());
        if (route.fallbacks() != null) models.addAll(route.fallbacks());

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("models", models);
        body.put("messages", messages.stream()
                .map(m -> Map.of("role", m.role(), "content", m.content())).toList());
        body.put("temperature", route.temperature());
        body.put("max_tokens", route.maxTokens());
        body.put("usage", Map.of("include", true));
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        long start = System.currentTimeMillis();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return parse(res, System.currentTimeMillis() - start);
        } catch (ResourceAccessException e) {
            // 타임아웃/커넥션 — 1회 재시도 후 504
            if (allowRetry) {
                sleep(1000);
                return doComplete(purpose, messages, jsonMode, false);
            }
            throw new DubbyException(ErrorCode.LLM_TIMEOUT, e.getMessage());
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (allowRetry && (status == 429 || status >= 500)) {
                sleep(1000);
                return doComplete(purpose, messages, jsonMode, false);
            }
            log.warn("OpenRouter {} 응답: {}", status, e.getResponseBodyAsString());
            throw new DubbyException(ErrorCode.LLM_UPSTREAM_ERROR, "OpenRouter HTTP " + status);
        }
    }

    @SuppressWarnings("unchecked")
    private LlmResult parse(Map<String, Object> res, long latencyMs) {
        if (res == null) throw new DubbyException(ErrorCode.LLM_UPSTREAM_ERROR, "Empty response");
        List<Map<String, Object>> choices = (List<Map<String, Object>>) res.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new DubbyException(ErrorCode.LLM_UPSTREAM_ERROR, "No choices");
        }
        Map<String, Object> choice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        String content = message != null ? String.valueOf(message.get("content")) : null;
        if (content == null || content.isBlank()) {
            throw new DubbyException(ErrorCode.LLM_UPSTREAM_ERROR, "Empty content");
        }
        Map<String, Object> usage = (Map<String, Object>) res.getOrDefault("usage", Map.of());
        return new LlmResult(
                content,
                String.valueOf(res.getOrDefault("model", "unknown")),
                toInt(usage.get("prompt_tokens")),
                toInt(usage.get("completion_tokens")),
                latencyMs,
                String.valueOf(choice.getOrDefault("finish_reason", "stop")));
    }

    private static int toInt(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
