package com.spring.dubbyserver.domain.task;

import com.spring.dubbyserver.domain.template.Template;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** 오늘의 업무 배정 — 쿨다운 원장 겸함. UNIQUE(user_id, assigned_date, slot) */
@Entity
@Table(name = "daily_task_assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyTaskAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(nullable = false)
    private short slot;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Reaction reaction;

    @Column(name = "reacted_at")
    private Instant reactedAt;

    @Column(name = "retry_count", nullable = false)
    private short retryCount;

    @Column(nullable = false)
    private boolean saved;

    @Column(nullable = false)
    private boolean shared;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DailyTaskAssignment(UUID userId, Template template, LocalDate assignedDate, int slot) {
        this.userId = userId;
        this.template = template;
        this.assignedDate = assignedDate;
        this.slot = (short) slot;
        this.retryCount = 0;
        this.saved = false;
        this.shared = false;
        this.createdAt = Instant.now();
    }

    /** 반응 덮어쓰기 허용 (마지막 반응 저장). RETRY는 카운트 증가 */
    public void react(Reaction reaction) {
        this.reaction = reaction;
        this.reactedAt = Instant.now();
        if (reaction == Reaction.RETRY) {
            this.retryCount++;
        }
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public void markShared() {
        this.shared = true;
    }

    public enum Reaction { PRAISE, SCOLD, RETRY, IGNORE }
}
