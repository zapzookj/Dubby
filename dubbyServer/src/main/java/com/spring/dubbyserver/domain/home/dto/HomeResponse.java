package com.spring.dubbyserver.domain.home.dto;

public record HomeResponse(
        Derby derby,
        TodayTasks todayTasks,
        ChatQuota chatQuota,
        Diary diary,
        Billing billing
) {
    public record Derby(String mood, String statusLine, String accuracy, String currentWork) {}

    public record TodayTasks(String date, int total, int reactedCount) {}

    public record ChatQuota(String tier, int limit, int used, int remaining) {}

    public record Diary(long totalEntries, long pendingCandidates) {}

    public record Billing(String tier, String expiresAt) {}
}
