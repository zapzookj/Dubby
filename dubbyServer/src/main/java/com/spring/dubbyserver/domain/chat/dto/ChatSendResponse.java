package com.spring.dubbyserver.domain.chat.dto;

import com.spring.dubbyserver.domain.chat.SafetyNotices;

/** kind: DERBY(일반) | SAFETY_NOTICE(고위험 — 시스템 카드) — derby_system_spec_v1.md §5.4 */
public record ChatSendResponse(
        String kind,
        ChatMessageDto userMessage,
        ChatMessageDto derbyMessage,
        DiaryCandidatePreview diaryCandidate,
        SafetyNotices.Notice safetyNotice,
        ChatQuotaDto quota
) {
    public record DiaryCandidatePreview(Long candidateId, String preview) {}

    public static ChatSendResponse derby(ChatMessageDto user, ChatMessageDto derby,
                                         DiaryCandidatePreview candidate, ChatQuotaDto quota) {
        return new ChatSendResponse("DERBY", user, derby, candidate, null, quota);
    }

    public static ChatSendResponse safety(SafetyNotices.Notice notice, ChatQuotaDto quota) {
        return new ChatSendResponse("SAFETY_NOTICE", null, null, null, notice, quota);
    }
}
