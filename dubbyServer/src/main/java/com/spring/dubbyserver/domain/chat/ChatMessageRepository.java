package com.spring.dubbyserver.domain.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByUserIdAndClientMsgId(UUID userId, String clientMsgId);

    Optional<ChatMessage> findByIdAndUserId(Long id, UUID userId);

    /** 멱등 재생용: 해당 사용자 메시지 직후의 더비 응답 */
    Optional<ChatMessage> findFirstByUserIdAndRoleAndIdGreaterThanOrderByIdAsc(
            UUID userId, ChatMessage.Role role, Long afterId);

    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.userId = :userId AND (:cursor IS NULL OR m.id < :cursor)
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findPage(@Param("userId") UUID userId, @Param("cursor") Long cursor, Pageable pageable);

    /** 컨텍스트 히스토리: 오늘(로컬 자정 이후) + safety 미플래그, 최신순 */
    @Query("""
            SELECT m FROM ChatMessage m
            WHERE m.userId = :userId AND m.safetyFlagged = false AND m.createdAt >= :since
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findHistorySince(@Param("userId") UUID userId, @Param("since") Instant since, Pageable pageable);

    /** 직전 더비 응답의 오해 강도 (연속 Lv.2 방지) */
    Optional<ChatMessage> findFirstByUserIdAndRoleOrderByIdDesc(UUID userId, ChatMessage.Role role);
}
