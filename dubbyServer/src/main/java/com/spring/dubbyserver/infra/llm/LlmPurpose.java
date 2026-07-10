package com.spring.dubbyserver.infra.llm;

/** 용도별 모델 라우팅 키 — yml dubby.llm.routing.{key}와 매핑 */
public enum LlmPurpose {
    CHAT("chat"),
    CHAT_PRECISE("chat-precise"),
    DIARY("diary"),
    TASK_FOLLOWUP("task-followup");

    private final String routingKey;

    LlmPurpose(String routingKey) {
        this.routingKey = routingKey;
    }

    public String routingKey() {
        return routingKey;
    }
}
