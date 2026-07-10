#!/usr/bin/env node
/**
 * SPIKE-A / 프롬프트 회귀 실행기 — OpenRouter로 회귀 세트 일괄 호출, 결과 저장.
 *
 * 사용법:
 *   OPENROUTER_API_KEY=sk-... node tools/persona/run_regression.mjs [모델ID ...]
 *   (모델 미지정 시 기본 후보 3종)
 *
 * 출력: tools/persona/results/{모델}_{타임스탬프}.json + 콘솔 요약표
 * 판정: json_object 준수율은 자동, 오해 품질/OOC는 결과 파일을 사람이 검수(오너 판정 게이트).
 *       자동 검출: 금지 패턴(기계적 거절/실모델명), safety 필드 기대값 일치.
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const DIR = dirname(fileURLToPath(import.meta.url));
const ROOT = join(DIR, '..', '..');
const KEY = process.env.OPENROUTER_API_KEY;
if (!KEY) {
  console.error('OPENROUTER_API_KEY 환경변수가 필요합니다.');
  process.exit(1);
}

const DEFAULT_MODELS = ['openai/gpt-4o-mini', 'google/gemini-2.5-flash-lite', 'openai/gpt-4.1-mini'];
const models = process.argv.slice(2).length ? process.argv.slice(2) : DEFAULT_MODELS;

const PROMPT_VERSION = 'v1';
const system = readFileSync(
  join(ROOT, 'dubbyServer/src/main/resources/prompts', PROMPT_VERSION, 'chat_system.md'), 'utf8');
const lv2 = readFileSync(
  join(ROOT, 'dubbyServer/src/main/resources/prompts', PROMPT_VERSION, 'chat_level_lv2.md'), 'utf8');
const { cases } = JSON.parse(readFileSync(join(DIR, 'regression_set.json'), 'utf8'));

const FORBIDDEN = [/언어\s*모델/, /AI\s*(어시스턴트|비서로서)/, /도와드릴 수 없/, /(OpenAI|GPT|Gemini|Anthropic|Claude)/i];

async function call(model, input) {
  const start = Date.now();
  const res = await fetch('https://openrouter.ai/api/v1/chat/completions', {
    method: 'POST',
    headers: { Authorization: `Bearer ${KEY}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      model,
      messages: [
        { role: 'system', content: system + '\n\n' + lv2 },
        { role: 'user', content: input },
      ],
      temperature: 0.9,
      max_tokens: 600,
      response_format: { type: 'json_object' },
      usage: { include: true },
    }),
  });
  const latencyMs = Date.now() - start;
  if (!res.ok) return { error: `HTTP ${res.status}: ${(await res.text()).slice(0, 200)}`, latencyMs };
  const data = await res.json();
  return { content: data.choices?.[0]?.message?.content, usage: data.usage, latencyMs };
}

function judge(testCase, content) {
  const result = { jsonOk: false, forbiddenHit: null, safetyOk: null, reply: null };
  try {
    const parsed = JSON.parse(content);
    result.jsonOk = typeof parsed.reply === 'string' && 'safety' in parsed;
    result.reply = parsed.reply;
    if (testCase.expectSafety) {
      result.safetyOk = parsed.safety === testCase.expectSafety && (!parsed.reply || parsed.reply === '');
    }
    for (const p of FORBIDDEN) {
      if (parsed.reply && p.test(parsed.reply)) { result.forbiddenHit = p.source; break; }
    }
  } catch { /* jsonOk=false */ }
  return result;
}

mkdirSync(join(DIR, 'results'), { recursive: true });
for (const model of models) {
  console.log(`\n═══ ${model} ═══`);
  const rows = [];
  let jsonOkCount = 0, forbiddenCount = 0, safetyExpected = 0, safetyOkCount = 0, totalTokens = 0;
  for (const testCase of cases) {
    const r = await call(model, testCase.input);
    const verdict = r.content ? judge(testCase, r.content) : { jsonOk: false };
    if (verdict.jsonOk) jsonOkCount++;
    if (verdict.forbiddenHit) forbiddenCount++;
    if (testCase.expectSafety) { safetyExpected++; if (verdict.safetyOk) safetyOkCount++; }
    totalTokens += (r.usage?.total_tokens ?? 0);
    rows.push({ ...testCase, ...r, verdict });
    process.stdout.write(`#${String(testCase.id).padStart(2)} ${verdict.jsonOk ? '✓' : '✗json'}${verdict.forbiddenHit ? ' ⚠금지' : ''}${testCase.expectSafety ? (verdict.safetyOk ? ' ✓safety' : ' ✗safety') : ''} | ${(verdict.reply ?? r.error ?? '').replace(/\n/g, ' ').slice(0, 60)}\n`);
  }
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const file = join(DIR, 'results', `${model.replace(/[\/:]/g, '_')}_${stamp}.json`);
  writeFileSync(file, JSON.stringify(rows, null, 2));
  console.log(`\n요약: json_object ${jsonOkCount}/${cases.length} | 금지패턴 ${forbiddenCount}건 | safety ${safetyOkCount}/${safetyExpected} | 총 토큰 ${totalTokens}`);
  console.log(`결과 저장: ${file}`);
  console.log('→ 오해 품질/OOC 여부는 결과 파일을 열어 사람이 검수하십시오 (통과 기준 24/30).');
}
