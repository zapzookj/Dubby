package com.spring.dubbyserver.domain.task.dto;

import java.util.List;

public record TaskItem(
        Long assignmentId,
        String templateCode,
        String title,
        String conclusion,
        String note,
        List<Button> buttons,
        String reaction,
        int retryCount,
        boolean saved
) {
    public record Button(String key, String label) {}
}
