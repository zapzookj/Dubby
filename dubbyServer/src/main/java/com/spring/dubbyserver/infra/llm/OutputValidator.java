package com.spring.dubbyserver.infra.llm;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/** OOC/형식 rule-based 검증 — LLM 심판 없음 (비용 원칙). 실패 시 1회 교정 재시도는 호출부 책임 */
@Component
public class OutputValidator {

    private static final int MAX_REPLY_LENGTH = 700;

    private static final List<Pattern> FORBIDDEN = List.of(
            Pattern.compile("언어\\s*모델"),
            Pattern.compile("AI\\s*(어시스턴트|비서로서)"),
            Pattern.compile("도와드릴 수 없"),
            Pattern.compile("죄송하지만.*불가능"),
            Pattern.compile("(OpenAI|GPT|Gemini|Anthropic|Claude|구글의)", Pattern.CASE_INSENSITIVE));

    public record Verdict(boolean valid, String sanitizedReply, String failReason) {}

    public Verdict validate(String reply) {
        if (reply == null || reply.isBlank()) {
            return new Verdict(false, null, "empty reply");
        }
        for (Pattern p : FORBIDDEN) {
            if (p.matcher(reply).find()) {
                return new Verdict(false, null, "forbidden pattern: " + p.pattern());
            }
        }
        String sanitized = reply
                .replaceAll("(?m)^#{1,3} ", "")   // 마크다운 헤더 제거
                .replace("```", "");
        if (sanitized.length() > MAX_REPLY_LENGTH) {
            sanitized = sanitized.substring(0, MAX_REPLY_LENGTH);
        }
        return new Verdict(true, sanitized, null);
    }
}
