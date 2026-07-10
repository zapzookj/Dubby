package com.spring.dubbyserver.infra.llm;

import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 로컬 개발용 모의 LLM (dubby.llm.mock=true) — OPENROUTER_API_KEY 없이 전체 파이프라인 검증.
 * 트리거: 입력에 "[mock:ooc]" → OOC 문구(검증 재시도 경로), "[mock:error]" → 업스트림 에러.
 * 페르소나 품질 검증용이 아님 — 실모델 검증은 SPIKE-A (tools/persona/).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "dubby.llm.mock", havingValue = "true")
public class MockLlmClient implements LlmClient {

    @Override
    public LlmResult complete(LlmPurpose purpose, List<Message> messages, boolean jsonMode) {
        String userInput = messages.stream()
                .filter(m -> "user".equals(m.role()))
                .reduce((a, b) -> b).map(Message::content).orElse("");
        String system = messages.isEmpty() ? "" : messages.get(0).content();

        if (userInput.contains("[mock:error]")) {
            throw new DubbyException(ErrorCode.LLM_UPSTREAM_ERROR, "mock upstream error");
        }

        String content;
        if (purpose == LlmPurpose.TASK_FOLLOWUP) {
            content = "재수행했습니다. 자신감이 1 상승했습니다. (모의 응답)";
        } else if (purpose == LlmPurpose.DIARY) {
            content = """
                    {"fact":"사용자님이 무언가를 알려주셨습니다.","interpretation":"모의 해석입니다. 실제 해석은 진짜 모델이 합니다.","conclusion":"그래도 기록은 남습니다."}""";
        } else {
            String reply = userInput.contains("[mock:ooc]")
                    ? "저는 AI 언어 모델이라서 도와드릴 수 없습니다."
                    : "「" + truncate(userInput, 40) + "」라는 요청을 접수했습니다.\\n접수가 제일 어려운 단계인데 벌써 성공했습니다. (모의 응답)";
            boolean withDiary = system.contains("[일기 후보 추출]") || messages.stream()
                    .anyMatch(m -> "system".equals(m.role()) && m.content().contains("[일기 후보 추출]"));
            String diary = withDiary
                    ? """
                      {"fact":"사용자님이 「%s」라고 하셨습니다.","interpretation":"중대한 자기 고백으로 분류했습니다. 근거는 소량입니다.","conclusion":"오늘도 인간에게 배웠습니다."}"""
                      .formatted(truncate(userInput.replace("\"", ""), 30))
                    : "null";
            content = """
                    {"reply":"%s","misreadType":"META","diary":%s,"safety":"NONE"}""".formatted(reply, diary);
        }
        return new LlmResult(content, "mock/derby-sim", estimate(messages), 80, 40, "stop");
    }

    private static int estimate(List<Message> messages) {
        return (int) (messages.stream().mapToInt(m -> m.content().length()).sum() * 0.7);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
