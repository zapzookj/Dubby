package com.spring.dubbyserver.domain.diary;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** 더비의 일기 — 삭제는 즉시 실삭제 (soft delete 없음, 스펙 §14) */
@Entity
@Table(name = "diary_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "candidate_id")
    private Long candidateId;

    @Column(nullable = false, length = 200)
    private String fact;

    @Column(nullable = false, length = 400)
    private String interpretation;

    @Column(nullable = false, length = 200)
    private String conclusion;

    @Column(name = "auto_saved", nullable = false)
    private boolean autoSaved;

    @Column(name = "is_shared", nullable = false)
    private boolean shared;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static DiaryEntry from(DiaryCandidate candidate, boolean autoSaved) {
        DiaryEntry e = new DiaryEntry();
        e.userId = candidate.getUserId();
        e.candidateId = candidate.getId();
        e.fact = candidate.getFact();
        e.interpretation = candidate.getInterpretation();
        e.conclusion = candidate.getConclusion();
        e.autoSaved = autoSaved;
        e.createdAt = Instant.now();
        return e;
    }

    public void markShared() {
        this.shared = true;
    }

    public void rewrite(String fact, String interpretation, String conclusion) {
        this.fact = fact;
        this.interpretation = interpretation;
        this.conclusion = conclusion;
    }
}
