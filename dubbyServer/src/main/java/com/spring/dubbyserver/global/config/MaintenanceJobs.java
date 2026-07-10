package com.spring.dubbyserver.global.config;

import com.spring.dubbyserver.domain.diary.DiaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 일일 유지보수 배치 — 데이터 보존 정책 (스펙 §14). 새벽 UTC 1회 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceJobs {

    private final DiaryService diaryService;
    private final JdbcClient jdbc;
    private final DubbyProperties properties;

    @Scheduled(cron = "0 30 18 * * *") // UTC 18:30 = KST 03:30
    public void dailyCleanup() {
        int candidates = diaryService.cleanupExpiredCandidates();

        long retentionDays = properties.retention().chatMessages().toDays();
        int chats = jdbc.sql("""
                DELETE FROM chat_messages
                WHERE is_saved = false AND created_at < now() - make_interval(days => :days)
                """)
                .param("days", (int) retentionDays)
                .update();

        int usage = jdbc.sql("""
                DELETE FROM chat_daily_usage WHERE usage_date < CURRENT_DATE - :days
                """)
                .param("days", (int) retentionDays)
                .update();

        log.info("일일 정리: 만료 일기후보 {}건, 채팅 {}건, 사용량 {}건 삭제", candidates, chats, usage);
    }
}
