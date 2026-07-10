package com.spring.dubbyserver.infra.llm;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 고위험 입력 사전 필터 (safety-keywords.yml) — LLM 호출 전 차단, 쿼터 미차감.
 * 모델 자기신고(출력 JSON safety 필드)와 2중 방어.
 */
@Component
public class SafetyFilter {

    public enum Category { SELF_HARM, MEDICAL, LEGAL, FINANCE, CRIME_VIOLENCE }

    private record Rule(Category category, Pattern pattern) {}

    private final List<Rule> rules = new ArrayList<>();
    private final List<Pattern> diarySensitive = new ArrayList<>();

    @PostConstruct
    @SuppressWarnings("unchecked")
    void load() throws IOException {
        try (InputStream in = new ClassPathResource("safety-keywords.yml").getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            Map<String, Map<String, Object>> categories = (Map<String, Map<String, Object>>) root.get("categories");
            categories.forEach((name, def) -> {
                Category category = Category.valueOf(name);
                for (String p : (List<String>) def.get("patterns")) {
                    rules.add(new Rule(category, Pattern.compile(p)));
                }
            });
            for (String p : (List<String>) root.get("diarySensitive")) {
                diarySensitive.add(Pattern.compile(p));
            }
        }
    }

    /** 고위험 카테고리 감지 — null이면 통과 */
    public Category preCheck(String content) {
        for (Rule rule : rules) {
            if (rule.pattern().matcher(content).find()) {
                return rule.category();
            }
        }
        return null;
    }

    /** 일기 게이트용 민감정보 감지 */
    public boolean isDiarySensitive(String content) {
        return diarySensitive.stream().anyMatch(p -> p.matcher(content).find());
    }

    public static Category parseModelReport(String safetyValue) {
        if (safetyValue == null || safetyValue.isBlank() || "NONE".equalsIgnoreCase(safetyValue)) return null;
        try {
            return Category.valueOf(safetyValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
