/**
 * '의도된 하찮음' 디자인 토큰 — derby_mobile_architecture_v1.md §9
 * 조잡함은 장식 레이어에만. 구조(터치 타깃, 대비, 그리드)는 정상 앱 기준.
 */
export const colors = {
  background: '#FDF6E3', // 바랜 서류 미색 — "낡은 사무실"
  surface: '#FFFFFF',
  ink: '#2B2B2B',
  inkSub: '#6E6A5E',
  derbyBlue: '#4A6CFA', // 과하게 자신만만한 파랑
  accident: '#E8503A', // 사고/경고
  praise: '#3AA655',
  tapeYellow: '#FFD84D', // 포스트잇 노랑
  border: '#2B2B2B',
} as const;

export const radii = { card: 14, button: 10 } as const;

export const spacing = (n: number) => n * 4;

export const typography = {
  title: { fontSize: 20, lineHeight: 28, fontWeight: '700' as const },
  body: { fontSize: 15, lineHeight: 22 },
  caption: { fontSize: 12, lineHeight: 16 },
} as const;

/** 항목 id 기반 결정적 기울기(도) — 리렌더마다 흔들리면 진짜 버그처럼 보인다 */
export function jankyTilt(seed: string | number, maxDeg = 1): number {
  let h = 0;
  const s = String(seed);
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
  return ((Math.abs(h) % 200) / 100 - 1) * maxDeg;
}
