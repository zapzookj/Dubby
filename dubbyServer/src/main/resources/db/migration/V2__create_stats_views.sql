-- 템플릿 성과 뷰 (스펙 §13) — 운영 판단(월 1회 수동): keep_rate 상위 → 변형 제작,
-- open_rate 하위 10% → RETIRED, SCOLD 압도 업무 → 페르소나 재검수

CREATE VIEW v_task_template_stats AS
SELECT t.id, t.code, t.category,
       count(a.id)                                   AS exposures,
       count(a.reaction)                             AS reacted,
       count(*) FILTER (WHERE a.reaction = 'PRAISE') AS praises,
       count(*) FILTER (WHERE a.reaction = 'SCOLD')  AS scolds,
       coalesce(sum(a.retry_count), 0)               AS retries,
       count(*) FILTER (WHERE a.saved)               AS saves,
       count(*) FILTER (WHERE a.shared)              AS shares,
       round(count(*) FILTER (WHERE a.saved OR a.shared)::numeric
             / nullif(count(a.id), 0), 4)            AS keep_rate
FROM templates t
LEFT JOIN daily_task_assignments a ON a.template_id = t.id
WHERE t.type = 'DAILY_TASK'
GROUP BY t.id, t.code, t.category;

CREATE VIEW v_push_template_stats AS
SELECT t.id, t.code, t.category,
       count(p.id) FILTER (WHERE p.status IN ('SENT','DELIVERED')) AS sent,
       count(p.opened_at)                                          AS opened,
       round(count(p.opened_at)::numeric
             / nullif(count(p.id) FILTER (WHERE p.status IN ('SENT','DELIVERED')), 0), 4) AS open_rate
FROM templates t
LEFT JOIN push_send_logs p ON p.template_id = t.id
WHERE t.type = 'PUSH'
GROUP BY t.id, t.code, t.category;

-- D1 리텐션 코호트 조회 예시 (수동 실행용 주석):
-- WITH cohort AS (SELECT id FROM users WHERE created_at::date = :cohortDate)
-- SELECT round(count(a.user_id)::numeric / nullif(count(c.id), 0), 4) AS d1
-- FROM cohort c LEFT JOIN user_activity_daily a
--   ON a.user_id = c.id AND a.activity_date = :cohortDate + 1;
