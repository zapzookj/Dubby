package com.spring.dubbyserver.domain.diary;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DiaryCandidateRepository extends JpaRepository<DiaryCandidate, Long> {

    Optional<DiaryCandidate> findByIdAndUserId(Long id, UUID userId);

    long countByUserIdAndCreatedAtGreaterThanEqual(UUID userId, Instant since);

    Optional<DiaryCandidate> findFirstByUserIdOrderByIdDesc(UUID userId);

    @Modifying
    @Query("DELETE FROM DiaryCandidate c WHERE c.status IN ('PENDING','REJECTED','EXPIRED') AND c.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}

interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {

    Optional<DiaryEntry> findByIdAndUserId(Long id, UUID userId);

    long countByUserId(UUID userId);

    @Query("""
            SELECT e FROM DiaryEntry e
            WHERE e.userId = :userId AND (:cursor IS NULL OR e.id < :cursor)
            ORDER BY e.id DESC
            """)
    List<DiaryEntry> findPage(@Param("userId") UUID userId, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT e FROM DiaryEntry e WHERE e.userId = :userId ORDER BY e.id DESC")
    List<DiaryEntry> findRecent(@Param("userId") UUID userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM DiaryEntry e WHERE e.userId = :userId")
    int deleteAllByUser(@Param("userId") UUID userId);
}
