package com.spring.dubbyserver.domain.diary;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "diary_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source_message_id")
    private Long sourceMessageId;

    @Column(nullable = false, length = 200)
    private String fact;

    @Column(nullable = false, length = 400)
    private String interpretation;

    @Column(nullable = false, length = 200)
    private String conclusion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DiaryCandidate(UUID userId, Long sourceMessageId,
                          String fact, String interpretation, String conclusion, Instant expiresAt) {
        this.userId = userId;
        this.sourceMessageId = sourceMessageId;
        this.fact = truncate(fact, 200);
        this.interpretation = truncate(interpretation, 400);
        this.conclusion = truncate(conclusion, 200);
        this.status = Status.PENDING;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public boolean isPendingAndAlive() {
        return status == Status.PENDING && expiresAt.isAfter(Instant.now());
    }

    public void approve() {
        this.status = Status.APPROVED;
    }

    public void reject() {
        this.status = Status.REJECTED;
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) : s;
    }

    public enum Status { PENDING, APPROVED, REJECTED, EXPIRED }
}
