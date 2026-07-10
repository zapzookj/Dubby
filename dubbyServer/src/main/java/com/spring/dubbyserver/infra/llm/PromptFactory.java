package com.spring.dubbyserver.infra.llm;

import com.spring.dubbyserver.global.config.DubbyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 프롬프트 리소스 로더 — resources/prompts/{version}/*.md
 * 프롬프트 튜닝 = 파일 수정(또는 새 버전 폴더) + yml prompt-version 변경.
 * local 프로파일에서는 캐시 없이 매 요청 재로드(재시작 없는 반복 튜닝).
 */
@Slf4j
@Component
public class PromptFactory {

    private final String version;
    private final boolean hotReload;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public PromptFactory(DubbyProperties properties, Environment env) {
        this.version = properties.llm().promptVersion();
        this.hotReload = java.util.Arrays.asList(env.getActiveProfiles()).contains("local");
    }

    public String promptVersion() {
        return version;
    }

    public String load(String name) {
        if (hotReload) {
            return read(name);
        }
        return cache.computeIfAbsent(name, this::read);
    }

    private String read(String name) {
        try {
            return new String(new ClassPathResource("prompts/" + version + "/" + name + ".md")
                    .getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new UncheckedIOException("프롬프트 로드 실패: " + name, e);
        }
    }
}
