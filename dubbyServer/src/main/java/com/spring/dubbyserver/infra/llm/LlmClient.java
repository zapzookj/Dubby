package com.spring.dubbyserver.infra.llm;

import java.util.List;

/** LLM 호출 단일 창구 인터페이스 — 구현: OpenRouterClient(운영) / MockLlmClient(dubby.llm.mock=true) */
public interface LlmClient {

    LlmResult complete(LlmPurpose purpose, List<Message> messages, boolean jsonMode);

    record Message(String role, String content) {
        public static Message system(String content) { return new Message("system", content); }
        public static Message user(String content) { return new Message("user", content); }
        public static Message assistant(String content) { return new Message("assistant", content); }
    }

    record LlmResult(String content, String model, int promptTokens, int completionTokens,
                     long latencyMs, String finishReason) {
        public int totalTokens() { return promptTokens + completionTokens; }
    }
}
