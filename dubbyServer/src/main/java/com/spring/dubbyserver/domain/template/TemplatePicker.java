package com.spring.dubbyserver.domain.template;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 템플릿 선정 로직의 단일 지점 (task/push 공용) — derby_system_spec_v1.md §8.5.
 * 쿨다운 원장: 오늘의 업무 = daily_task_assignments, 푸시 = push_send_logs.
 */
@Component
@RequiredArgsConstructor
public class TemplatePicker {

    private final JdbcClient jdbc;

    public record Candidate(long templateId, String code, String category, String intensity, boolean wasKept) {}

    /** 저장/공유했던 템플릿은 재노출 가중치 하향 (기획 §8.2) */
    private static final double KEPT_WEIGHT = 0.3;

    // ── 오늘의 업무 ──

    /**
     * 쿨다운·프리미엄 필터 통과 후보 → 가중 셔플 + 카테고리 중복 회피 + HIGH 하루 1개 제한으로 count개 선정.
     * 풀 고갈 시 2단계 완화: ① 카테고리 제약 해제 ② 쿨다운 무시 least-recently-shown.
     */
    public List<Candidate> pickDailyTasks(java.util.UUID userId, LocalDate localDate,
                                          boolean isPremium, String locale, int count) {
        List<Candidate> pool = jdbc.sql("""
                SELECT t.id, t.code, t.category, t.intensity,
                       EXISTS (SELECT 1 FROM daily_task_assignments h
                               WHERE h.user_id = :userId AND h.template_id = t.id
                                 AND (h.saved OR h.shared)) AS was_kept
                FROM templates t
                WHERE t.type = 'DAILY_TASK' AND t.status = 'ACTIVE' AND t.locale = :locale
                  AND (t.is_premium = FALSE OR :isPremium)
                  AND NOT EXISTS (
                      SELECT 1 FROM daily_task_assignments a
                      WHERE a.user_id = :userId AND a.template_id = t.id
                        AND a.assigned_date > (CAST(:localDate AS date) - t.cooldown_days))
                """)
                .param("userId", userId)
                .param("locale", locale)
                .param("isPremium", isPremium)
                .param("localDate", localDate)
                .query((rs, i) -> new Candidate(rs.getLong("id"), rs.getString("code"),
                        rs.getString("category"), rs.getString("intensity"), rs.getBoolean("was_kept")))
                .list();

        List<Candidate> ordered = weightedShuffle(pool);

        // 1차: 카테고리 중복 회피 + HIGH 하루 1개
        List<Candidate> picked = new ArrayList<>(count);
        Set<String> usedCategories = new HashSet<>();
        int highCount = 0;
        for (Candidate c : ordered) {
            if (picked.size() == count) break;
            if (usedCategories.contains(c.category())) continue;
            if ("HIGH".equals(c.intensity()) && highCount >= 1) continue;
            picked.add(c);
            usedCategories.add(c.category());
            if ("HIGH".equals(c.intensity())) highCount++;
        }

        // 완화 ①: 카테고리 제약 해제
        if (picked.size() < count) {
            for (Candidate c : ordered) {
                if (picked.size() == count) break;
                if (picked.stream().anyMatch(p -> p.templateId() == c.templateId())) continue;
                picked.add(c);
            }
        }

        // 완화 ②: 쿨다운 무시, 가장 오래전에 본 순 (템플릿 풀 30개 규모에서 필수)
        if (picked.size() < count) {
            List<Long> exclude = picked.stream().map(Candidate::templateId).toList();
            picked.addAll(pickLeastRecentlyShown(userId, locale, isPremium, exclude, count - picked.size()));
        }
        return picked;
    }

    private List<Candidate> pickLeastRecentlyShown(java.util.UUID userId, String locale, boolean isPremium,
                                                   List<Long> excludeIds, int limit) {
        return jdbc.sql("""
                SELECT t.id, t.code, t.category, t.intensity, FALSE AS was_kept
                FROM templates t
                WHERE t.type = 'DAILY_TASK' AND t.status = 'ACTIVE' AND t.locale = :locale
                  AND (t.is_premium = FALSE OR :isPremium)
                  AND (:excludeEmpty OR t.id NOT IN (:excludeIds))
                ORDER BY (SELECT MAX(a.assigned_date) FROM daily_task_assignments a
                          WHERE a.user_id = :userId AND a.template_id = t.id) ASC NULLS FIRST,
                         random()
                LIMIT :limit
                """)
                .param("userId", userId)
                .param("locale", locale)
                .param("isPremium", isPremium)
                .param("excludeEmpty", excludeIds.isEmpty())
                .param("excludeIds", excludeIds.isEmpty() ? List.of(-1L) : excludeIds)
                .param("limit", limit)
                .query((rs, i) -> new Candidate(rs.getLong("id"), rs.getString("code"),
                        rs.getString("category"), rs.getString("intensity"), false))
                .list();
    }

    /** 가중 셔플: kept(저장/공유 이력) 템플릿은 KEPT_WEIGHT 배율로 후순위 경향 */
    private List<Candidate> weightedShuffle(List<Candidate> pool) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        return pool.stream()
                .map(c -> new Object[]{c, Math.pow(rnd.nextDouble(), 1.0 / (c.wasKept() ? KEPT_WEIGHT : 1.0))})
                .sorted((a, b) -> Double.compare((double) b[1], (double) a[1]))
                .map(arr -> (Candidate) arr[0])
                .toList();
    }
}
