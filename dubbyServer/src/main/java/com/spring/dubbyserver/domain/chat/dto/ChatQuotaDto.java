package com.spring.dubbyserver.domain.chat.dto;

/** tier/resetsAt/exhaustedMessage는 /chat/quota에서만 채워짐 (전송 응답에는 null) */
public record ChatQuotaDto(
        String tier,
        int limit,
        int used,
        int remaining,
        String resetsAt,
        String exhaustedMessage
) {}
