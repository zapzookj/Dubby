package com.spring.dubbyserver.domain.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 서버 발생 이벤트 기록 (app_events) — MVP 최소셋. 클라 이벤트 파이프라인은 post-MVP.
 * properties에 자유 텍스트(채팅/일기 본문) 금지 — ID/코드 참조만 (스펙 §13).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppEventRecorder {

    private final JdbcClient jdbc;

    public void record(UUID userId, String eventType, String targetType, Long targetId,
                       Map<String, Object> properties) {
        try {
            jdbc.sql("""
                    INSERT INTO app_events (user_id, event_type, target_type, target_id, properties)
                    VALUES (:userId, :eventType, :targetType, :targetId, CAST(:props AS jsonb))
                    """)
                    .param("userId", userId)
                    .param("eventType", eventType)
                    .param("targetType", targetType)
                    .param("targetId", targetId)
                    .param("props", toJson(properties))
                    .update();
        } catch (Exception e) {
            // 지표 기록 실패가 본 기능을 깨면 안 된다
            log.warn("app_event 기록 실패: {} {}", eventType, e.getMessage());
        }
    }

    public void record(UUID userId, String eventType, String targetType, Long targetId) {
        record(userId, eventType, targetType, targetId, Map.of());
    }

    private static String toJson(Map<String, Object> props) {
        if (props == null || props.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : props.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append('"').append(String.valueOf(v).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            }
        }
        return sb.append('}').toString();
    }
}
