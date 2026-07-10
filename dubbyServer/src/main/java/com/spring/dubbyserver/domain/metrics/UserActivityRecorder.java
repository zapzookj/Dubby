package com.spring.dubbyserver.domain.metrics;

import com.spring.dubbyserver.global.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * D1/D7 리텐션용 user_activity_daily upsert 인터셉터.
 * 인메모리 캐시로 (user, date)당 DB 1회만. 날짜는 UTC 기준(리텐션 집계용 — 정밀 타임존 불필요).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityRecorder implements HandlerInterceptor {

    private final JdbcClient jdbc;
    private final Map<String, Boolean> recordedToday = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user) {
            LocalDate today = LocalDate.now();
            String key = user.id() + ":" + today;
            if (recordedToday.putIfAbsent(key, Boolean.TRUE) == null) {
                if (recordedToday.size() > 50_000) {
                    recordedToday.clear(); // 단순 메모리 가드 — 재기록되어도 upsert 멱등
                }
                upsert(user.id(), today);
            }
        }
        return true;
    }

    private void upsert(UUID userId, LocalDate date) {
        try {
            jdbc.sql("""
                    INSERT INTO user_activity_daily (user_id, activity_date)
                    VALUES (:userId, :date) ON CONFLICT DO NOTHING
                    """)
                    .param("userId", userId)
                    .param("date", date)
                    .update();
        } catch (Exception e) {
            log.warn("user_activity_daily 기록 실패: {}", e.getMessage());
        }
    }
}
