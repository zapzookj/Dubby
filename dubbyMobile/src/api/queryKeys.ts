/** TanStack Query 키 팩토리 — 전역 단일 소스 */
export const queryKeys = {
  home: ['home'] as const,
  tasksToday: (localDate: string) => ['tasks', 'today', localDate] as const,
  tasksSaved: ['tasks', 'saved'] as const,
  chatQuota: ['chat', 'quota'] as const,
  chatMessages: ['chat', 'messages'] as const,
  diary: ['diary'] as const,
  settings: ['settings'] as const,
  billing: ['billing'] as const,
} as const;

/** 유저 로컬 날짜(YYYY-MM-DD) — 일일 리셋 캐시 키용. 진실은 서버 */
export function localDateString(d = new Date()): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
