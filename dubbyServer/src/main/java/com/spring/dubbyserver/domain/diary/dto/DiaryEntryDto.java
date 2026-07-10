package com.spring.dubbyserver.domain.diary.dto;

import com.spring.dubbyserver.domain.diary.DiaryEntry;

public record DiaryEntryDto(
        Long entryId,
        String fact,
        String interpretation,
        String conclusion,
        boolean autoSaved,
        boolean isShared,
        String createdAt
) {
    public static DiaryEntryDto from(DiaryEntry e) {
        return new DiaryEntryDto(e.getId(), e.getFact(), e.getInterpretation(), e.getConclusion(),
                e.isAutoSaved(), e.isShared(), e.getCreatedAt().toString());
    }
}
