package com.spring.dubbyserver.domain.chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    @Column(nullable = false)
    private String content;

    @Column(length = 80)
    private String model;

    @Column(name = "misread_level")
    private Short misreadLevel;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "client_msg_id", length = 64)
    private String clientMsgId;

    @Column(name = "is_saved", nullable = false)
    private boolean saved;

    @Column(name = "safety_flagged", nullable = false)
    private boolean safetyFlagged;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ChatMessage user(UUID userId, String content, String clientMsgId, boolean safetyFlagged) {
        ChatMessage m = new ChatMessage();
        m.userId = userId;
        m.role = Role.USER;
        m.content = content;
        m.clientMsgId = clientMsgId;
        m.safetyFlagged = safetyFlagged;
        m.createdAt = Instant.now();
        return m;
    }

    public static ChatMessage derby(UUID userId, String content, String model,
                                    int misreadLevel, int promptTokens, int completionTokens) {
        ChatMessage m = new ChatMessage();
        m.userId = userId;
        m.role = Role.DERBY;
        m.content = content;
        m.model = model;
        m.misreadLevel = (short) misreadLevel;
        m.promptTokens = promptTokens;
        m.completionTokens = completionTokens;
        m.createdAt = Instant.now();
        return m;
    }

    public void markSaved() {
        this.saved = true;
    }

    public enum Role { USER, DERBY }
}
