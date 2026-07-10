package com.spring.dubbyserver.domain.template;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 템플릿 원장 (업무/푸시/홈상태 공용). 시딩은 R__seed_templates.sql — 코드에서 쓰기 금지 */
@Entity
@Table(name = "templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TemplateType type;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(name = "time_window", nullable = false, length = 16)
    private String timeWindow;

    @Column(nullable = false, length = 8)
    private String intensity;

    @Column(name = "requires_user_name", nullable = false)
    private boolean requiresUserName;

    @Column(name = "is_premium", nullable = false)
    private boolean premium;

    @Column(name = "cooldown_days", nullable = false)
    private short cooldownDays;

    @Column(nullable = false, length = 8)
    private String locale;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> content;

    @Column(nullable = false, length = 8)
    private String status;

    @Column(name = "content_version", nullable = false)
    private int contentVersion;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public enum TemplateType { DAILY_TASK, PUSH, HOME_STATUS }

    // ── content JSONB 접근 헬퍼 ──

    public String contentString(String key) {
        Object v = content.get(key);
        return v == null ? null : String.valueOf(v);
    }

    /** content.buttons[key].responses (없으면 null — 기본 풀 폴백) */
    @SuppressWarnings("unchecked")
    public List<String> buttonResponses(String buttonKey) {
        Object buttons = content.get("buttons");
        if (!(buttons instanceof Map<?, ?> buttonMap)) return null;
        Object button = buttonMap.get(buttonKey);
        if (!(button instanceof Map<?, ?> b)) return null;
        Object responses = b.get("responses");
        return responses instanceof List<?> list ? (List<String>) list : null;
    }
}
