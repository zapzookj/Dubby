package com.spring.dubbyserver.domain.task.dto;

public record SavedTaskItem(
        Long assignmentId,
        String templateCode,
        String title,
        String conclusion,
        String note,
        String assignedDate
) {}
