package com.spring.dubbyserver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 컨텍스트 로드 테스트는 로컬 PostgreSQL(docker compose)이 필요하므로
 * 통합 테스트 도입 시(P1+) Testcontainers 또는 compose 전제 프로파일로 재구성한다.
 */
class DubbyServerApplicationTests {

    @Test
    void placeholder() {
        assertTrue(true);
    }
}
