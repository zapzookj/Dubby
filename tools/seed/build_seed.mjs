#!/usr/bin/env node
/**
 * 더비 템플릿 시드 생성기 — docs 마크다운(SSOT) → Flyway R__seed_templates.sql
 *
 * 사용법: node tools/seed/build_seed.mjs   (레포 루트에서)
 *
 * 입력:
 *   docs/derby_daily_tasks_top30_v2.md      (### TASK-NNN / ✅제목 / 결론: / 비고:)
 *   docs/derby_push_notifications_top30_v1.md (## PUSH-NNN / **제목:** / **본문:**)
 *   docs/derby_home_status_v1.md            (## HOME-NNN / **상태:** / **업무:**)
 *
 * 메타 오버라이드(선택): 헤딩 바로 아래 HTML 주석
 *   <!-- meta: category=ANALYSIS, intensity=MID, cooldown=60, premium=false, time_window=ANY -->
 *
 * 신규 템플릿은 meta 주석 필수(초기 30/30은 아래 내장 매핑 사용).
 * 검증 실패 시 exit 1. 생성물은 git에 커밋한다(마이그레이션 재현성).
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const OUT = join(ROOT, 'dubbyServer', 'src', 'main', 'resources', 'db', 'migration', 'R__seed_templates.sql');

// 쿨다운 기본값 — dubbyServer application.yml의 dubby.task/push.default-cooldown-days와 일치 유지
const DEFAULT_COOLDOWN = { DAILY_TASK: 60, PUSH: 30, HOME_STATUS: 0 };

const CATEGORIES = {
  DAILY_TASK: ['ANALYSIS', 'SCHEDULE', 'SELF_IMPROVE', 'USER_OBSERVE', 'META_REPORT'],
  PUSH: ['HELP_REQUEST', 'INCIDENT_REPORT', 'FAKE_URGENT', 'WORK_DONE', 'SELF_PRAISE', 'SEPARATION_ANXIETY', 'NO_CONTENT'],
  HOME_STATUS: ['HOME_STATUS'],
};

/** 초기 30/30 카테고리/시간창 내장 매핑 (신규 템플릿은 meta 주석으로 지정) */
const BUILT_IN_META = {
  'TASK-001': { category: 'ANALYSIS' }, 'TASK-002': { category: 'ANALYSIS' },
  'TASK-003': { category: 'SELF_IMPROVE' }, 'TASK-004': { category: 'ANALYSIS' },
  'TASK-005': { category: 'SCHEDULE' }, 'TASK-006': { category: 'META_REPORT' },
  'TASK-007': { category: 'USER_OBSERVE' }, 'TASK-008': { category: 'SELF_IMPROVE' },
  'TASK-009': { category: 'SCHEDULE' }, 'TASK-010': { category: 'USER_OBSERVE' },
  'TASK-011': { category: 'ANALYSIS' }, 'TASK-012': { category: 'ANALYSIS' },
  'TASK-013': { category: 'SCHEDULE' }, 'TASK-014': { category: 'SCHEDULE' },
  'TASK-015': { category: 'SCHEDULE' }, 'TASK-016': { category: 'SELF_IMPROVE' },
  'TASK-017': { category: 'SELF_IMPROVE' }, 'TASK-018': { category: 'ANALYSIS' },
  'TASK-019': { category: 'USER_OBSERVE', time_window: 'EVENING' },
  'TASK-020': { category: 'META_REPORT' }, 'TASK-021': { category: 'META_REPORT' },
  'TASK-022': { category: 'META_REPORT' }, 'TASK-023': { category: 'USER_OBSERVE' },
  'TASK-024': { category: 'SELF_IMPROVE' }, 'TASK-025': { category: 'ANALYSIS' },
  'TASK-026': { category: 'USER_OBSERVE' }, 'TASK-027': { category: 'SCHEDULE' },
  'TASK-028': { category: 'META_REPORT' }, 'TASK-029': { category: 'USER_OBSERVE' },
  'TASK-030': { category: 'META_REPORT' },
  'PUSH-001': { category: 'NO_CONTENT' }, 'PUSH-002': { category: 'WORK_DONE' },
  'PUSH-003': { category: 'SELF_PRAISE' }, 'PUSH-004': { category: 'SELF_PRAISE' },
  'PUSH-005': { category: 'NO_CONTENT' }, 'PUSH-006': { category: 'SELF_PRAISE' },
  'PUSH-007': { category: 'FAKE_URGENT' }, 'PUSH-008': { category: 'FAKE_URGENT' },
  'PUSH-009': { category: 'SEPARATION_ANXIETY', time_window: 'EVENING' },
  'PUSH-010': { category: 'WORK_DONE' }, 'PUSH-011': { category: 'HELP_REQUEST' },
  'PUSH-012': { category: 'INCIDENT_REPORT' }, 'PUSH-013': { category: 'INCIDENT_REPORT' },
  'PUSH-014': { category: 'FAKE_URGENT' }, 'PUSH-015': { category: 'WORK_DONE' },
  'PUSH-016': { category: 'WORK_DONE' }, 'PUSH-017': { category: 'INCIDENT_REPORT' },
  'PUSH-018': { category: 'SELF_PRAISE' }, 'PUSH-019': { category: 'WORK_DONE', time_window: 'MORNING' },
  'PUSH-020': { category: 'SELF_PRAISE' }, 'PUSH-021': { category: 'NO_CONTENT' },
  'PUSH-022': { category: 'SELF_PRAISE' }, 'PUSH-023': { category: 'INCIDENT_REPORT' },
  'PUSH-024': { category: 'SELF_PRAISE' }, 'PUSH-025': { category: 'HELP_REQUEST' },
  'PUSH-026': { category: 'SELF_PRAISE' }, 'PUSH-027': { category: 'WORK_DONE' },
  'PUSH-028': { category: 'SELF_PRAISE' }, 'PUSH-029': { category: 'WORK_DONE', time_window: 'MORNING' },
  'PUSH-030': { category: 'NO_CONTENT' },
};

const errors = [];
const err = (code, msg) => errors.push(`[${code}] ${msg}`);

function read(rel) {
  return readFileSync(join(ROOT, 'docs', rel), 'utf8');
}

function parseMetaComment(block) {
  const m = block.match(/<!--\s*meta:\s*([^>]*?)\s*-->/);
  if (!m) return {};
  const meta = {};
  for (const pair of m[1].split(',')) {
    const [k, v] = pair.split('=').map((s) => s.trim());
    if (!k || v === undefined) continue;
    if (k === 'cooldown') meta.cooldown_days = Number(v);
    else if (k === 'premium') meta.is_premium = v === 'true';
    else meta[k] = v;
  }
  return meta;
}

/** 코드 헤딩(정규식)으로 문서를 블록 분할 */
function splitBlocks(src, headingRe) {
  const blocks = [];
  const re = new RegExp(headingRe, 'gm');
  let match;
  const positions = [];
  while ((match = re.exec(src)) !== null) {
    positions.push({ code: match[1], start: match.index + match[0].length });
  }
  for (let i = 0; i < positions.length; i++) {
    const end = i + 1 < positions.length ? src.indexOf(positions[i + 1].code, positions[i].start) - 4 : src.length;
    blocks.push({ code: positions[i].code, body: src.slice(positions[i].start, end) });
  }
  return blocks;
}

function firstLineMatching(block, re) {
  for (const raw of block.split('\n')) {
    const line = raw.trim();
    const m = line.match(re);
    if (m) return m[1].trim();
  }
  return null;
}

// ── DAILY_TASK 파싱 ──
function parseTasks() {
  const src = read('derby_daily_tasks_top30_v2.md');
  return splitBlocks(src, '^### (TASK-\\d{3})\\s*$').map(({ code, body }) => {
    const meta = { ...BUILT_IN_META[code], ...parseMetaComment(body) };
    const title = firstLineMatching(body, /^✅\s*(.+)$/);
    const conclusion = firstLineMatching(body, /^결론:\s*(.+)$/);
    let note = firstLineMatching(body, /^비고:\s*(.*)$/);
    if (note === '') note = null;
    if (!title) err(code, 'title(✅ 라인) 누락');
    if (!conclusion) err(code, '결론: 누락');
    return {
      code, type: 'DAILY_TASK', meta,
      content: {
        title, conclusion, note,
        shareText: `더비의 업무 보고: ${title ?? ''}\n결론: ${conclusion ?? ''}`,
      },
    };
  });
}

// ── PUSH 파싱 ──
const PUSH_DEEPLINK_BY_CATEGORY = {
  NO_CONTENT: 'dubby://home', HELP_REQUEST: 'dubby://home', SEPARATION_ANXIETY: 'dubby://home',
  // 나머지는 오늘의 업무로
};
function parsePushes() {
  const src = read('derby_push_notifications_top30_v1.md');
  return splitBlocks(src, '^## (PUSH-\\d{3})\\s*$').map(({ code, body }) => {
    const meta = { ...BUILT_IN_META[code], ...parseMetaComment(body) };
    const title = firstLineMatching(body, /^\*\*제목:\*\*\s*(.+)$/);
    const pushBody = firstLineMatching(body, /^\*\*본문:\*\*\s*(.+)$/);
    if (!title) err(code, '**제목:** 누락');
    else if (title.length > 20) err(code, `제목 20자 초과(${title.length}자) — 잠금화면 잘림`);
    if (!pushBody) err(code, '**본문:** 누락');
    else if (pushBody.length > 100) err(code, `본문 100자 초과(${pushBody.length}자)`);
    return {
      code, type: 'PUSH', meta,
      content: {
        title, body: pushBody,
        deeplink: PUSH_DEEPLINK_BY_CATEGORY[meta.category] ?? 'dubby://tasks',
      },
    };
  });
}

// ── HOME_STATUS 파싱 ──
function parseHomeStatuses() {
  const src = read('derby_home_status_v1.md');
  return splitBlocks(src, '^## (HOME-\\d{3})\\s*$').map(({ code, body }) => {
    const meta = { category: 'HOME_STATUS', ...parseMetaComment(body) };
    const statusLine = firstLineMatching(body, /^\*\*상태:\*\*\s*(.+)$/);
    const currentWork = firstLineMatching(body, /^\*\*업무:\*\*\s*(.+)$/);
    if (!statusLine) err(code, '**상태:** 누락');
    else if (statusLine.length > 20) err(code, `상태 20자 초과(${statusLine.length}자)`);
    if (!currentWork) err(code, '**업무:** 누락');
    else if (currentWork.length > 20) err(code, `업무 20자 초과(${currentWork.length}자)`);
    return { code, type: 'HOME_STATUS', meta, content: { statusLine, currentWork } };
  });
}

// ── 공통 검증 + row 구성 ──
function toRow(t) {
  const meta = t.meta ?? {};
  const category = meta.category;
  if (!category) err(t.code, 'category 미지정 (신규 템플릿은 meta 주석 필수)');
  else if (!CATEGORIES[t.type].includes(category)) err(t.code, `알 수 없는 category: ${category}`);
  const timeWindow = meta.time_window ?? 'ANY';
  if (!['ANY', 'MORNING', 'LUNCH', 'EVENING'].includes(timeWindow)) err(t.code, `time_window 위반: ${timeWindow}`);
  const intensity = meta.intensity ?? 'LOW';
  if (!['LOW', 'MID', 'HIGH'].includes(intensity)) err(t.code, `intensity 위반: ${intensity}`);

  const bodyText = JSON.stringify(t.content);
  const requiresUserName = bodyText.includes('{nickname}');
  if (meta.requires_user_name === true && !requiresUserName) err(t.code, 'requires_user_name=true인데 {nickname} 치환자 없음');

  return {
    code: t.code,
    type: t.type,
    category,
    time_window: timeWindow,
    intensity,
    requires_user_name: requiresUserName,
    is_premium: meta.is_premium ?? false,
    cooldown_days: meta.cooldown_days ?? DEFAULT_COOLDOWN[t.type],
    content: t.content,
  };
}

// ── SQL 생성 ──
const q = (s) => `'${String(s).replace(/'/g, "''")}'`;
const qj = (obj) => `${q(JSON.stringify(obj))}::jsonb`;

function buildSql(rows) {
  const values = rows.map((r) =>
    `(${q(r.code)}, ${q(r.type)}, ${q(r.category)}, ${q(r.time_window)}, ${q(r.intensity)}, ` +
    `${r.requires_user_name}, ${r.is_premium}, ${r.cooldown_days}, 'ko', '{}', ${qj(r.content)})`,
  );

  const codesByType = {};
  for (const r of rows) (codesByType[r.type] ??= []).push(q(r.code));

  const retireBlocks = Object.entries(codesByType).map(([type, codes]) => `
-- docs에서 제거된 ${type} 템플릿 폐기 (물리 삭제 금지 — 이력/지표 보존), 복귀 시 재활성화
UPDATE templates SET status = 'RETIRED', updated_at = now()
WHERE type = ${q(type)} AND status = 'ACTIVE' AND code NOT IN (${codes.join(', ')});
UPDATE templates SET status = 'ACTIVE', updated_at = now()
WHERE type = ${q(type)} AND status = 'RETIRED' AND code IN (${codes.join(', ')});`).join('\n');

  return `-- ============================================================================
-- R__seed_templates.sql — tools/seed/build_seed.mjs 생성물. 직접 수정 금지.
-- 소스(SSOT): docs/derby_daily_tasks_top30_v2.md,
--             docs/derby_push_notifications_top30_v1.md,
--             docs/derby_home_status_v1.md
-- Flyway repeatable: 파일 체크섬 변경 시 자동 재실행 (upsert 멱등)
-- ============================================================================

INSERT INTO templates (code, type, category, time_window, intensity,
                       requires_user_name, is_premium, cooldown_days, locale, tags, content)
VALUES
${values.join(',\n')}
ON CONFLICT (code) DO UPDATE SET
    type = EXCLUDED.type,
    category = EXCLUDED.category,
    time_window = EXCLUDED.time_window,
    intensity = EXCLUDED.intensity,
    requires_user_name = EXCLUDED.requires_user_name,
    is_premium = EXCLUDED.is_premium,
    cooldown_days = EXCLUDED.cooldown_days,
    content = EXCLUDED.content,
    content_version = templates.content_version + 1,
    updated_at = now()
WHERE (templates.content, templates.category, templates.time_window, templates.intensity,
       templates.is_premium, templates.cooldown_days)
      IS DISTINCT FROM
      (EXCLUDED.content, EXCLUDED.category, EXCLUDED.time_window, EXCLUDED.intensity,
       EXCLUDED.is_premium, EXCLUDED.cooldown_days);
${retireBlocks}
`;
}

// ── main ──
const rows = [...parseTasks(), ...parsePushes(), ...parseHomeStatuses()].map(toRow);

const seen = new Set();
for (const r of rows) {
  if (!/^(TASK|PUSH|HOME)-\d{3}$/.test(r.code)) err(r.code, '코드 형식 위반');
  if (seen.has(r.code)) err(r.code, '코드 중복');
  seen.add(r.code);
}

if (errors.length > 0) {
  console.error(`시드 생성 실패 — ${errors.length}건:\n` + errors.join('\n'));
  process.exit(1);
}

mkdirSync(dirname(OUT), { recursive: true });
writeFileSync(OUT, buildSql(rows), 'utf8');

const counts = rows.reduce((acc, r) => ((acc[r.type] = (acc[r.type] ?? 0) + 1), acc), {});
console.log(`OK: ${rows.length}개 템플릿 → ${OUT}`);
console.log(Object.entries(counts).map(([t, n]) => `${t}=${n}`).join(', '));
