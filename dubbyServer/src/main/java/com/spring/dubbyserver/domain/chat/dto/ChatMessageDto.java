package com.spring.dubbyserver.domain.chat.dto;

import com.spring.dubbyserver.domain.chat.ChatMessage;

public record ChatMessageDto(Long id, String role, String content, boolean saved, String createdAt) {

    public static ChatMessageDto from(ChatMessage m) {
        return new ChatMessageDto(m.getId(), m.getRole().name(), m.getContent(), m.isSaved(),
                m.getCreatedAt().toString());
    }
}
