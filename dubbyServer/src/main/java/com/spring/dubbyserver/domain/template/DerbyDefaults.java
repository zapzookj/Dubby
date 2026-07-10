package com.spring.dubbyserver.domain.template;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * derby-defaults.yml 로더 — 버튼 라벨/기본 후속반응 풀/폴백 문구.
 * 문구 튜닝 = yml 수정 + 재배포 (DB 테이블 만들지 않는다).
 */
@Component
public class DerbyDefaults {

    private Map<String, String> buttonLabels;
    private Map<String, List<String>> buttonResponses;
    private String retryExhausted;
    private List<String> saveConfirm;
    private String unsaveConfirm;
    private String shareSuffix;
    private List<String> llmFallback;

    @PostConstruct
    @SuppressWarnings("unchecked")
    void load() throws IOException {
        try (InputStream in = new ClassPathResource("derby-defaults.yml").getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            buttonLabels = (Map<String, String>) root.get("buttonLabels");
            buttonResponses = (Map<String, List<String>>) root.get("buttonResponses");
            retryExhausted = (String) root.get("retryExhausted");
            saveConfirm = (List<String>) root.get("saveConfirm");
            unsaveConfirm = (String) root.get("unsaveConfirm");
            shareSuffix = (String) root.get("shareSuffix");
            llmFallback = (List<String>) root.get("llmFallback");
        }
    }

    public Map<String, String> buttonLabels() {
        return buttonLabels;
    }

    public String label(String buttonKey) {
        return buttonLabels.getOrDefault(buttonKey, buttonKey);
    }

    public String defaultResponse(String buttonKey) {
        return pick(buttonResponses.get(buttonKey));
    }

    public String retryExhausted() {
        return retryExhausted;
    }

    public String saveConfirm() {
        return pick(saveConfirm);
    }

    public String unsaveConfirm() {
        return unsaveConfirm;
    }

    public String shareSuffix() {
        return shareSuffix;
    }

    public String llmFallback() {
        return pick(llmFallback);
    }

    private static String pick(List<String> pool) {
        if (pool == null || pool.isEmpty()) return "더비가 할 말을 잃었습니다. 곧 찾겠습니다.";
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }
}
