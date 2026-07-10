package com.spring.dubbyserver.domain.task;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyTaskAssignmentRepository extends JpaRepository<DailyTaskAssignment, Long> {

    @EntityGraph(attributePaths = "template")
    List<DailyTaskAssignment> findByUserIdAndAssignedDateOrderBySlotAsc(UUID userId, LocalDate assignedDate);

    @EntityGraph(attributePaths = "template")
    Optional<DailyTaskAssignment> findByIdAndUserId(Long id, UUID userId);

    long countByUserIdAndAssignedDateAndReactionIsNotNull(UUID userId, LocalDate assignedDate);

    @EntityGraph(attributePaths = "template")
    @Query("""
            SELECT a FROM DailyTaskAssignment a
            WHERE a.userId = :userId AND a.saved = true AND (:cursor IS NULL OR a.id < :cursor)
            ORDER BY a.id DESC
            """)
    List<DailyTaskAssignment> findSavedPage(@Param("userId") UUID userId,
                                            @Param("cursor") Long cursor,
                                            Pageable pageable);
}
