import { QueryClient, focusManager } from '@tanstack/react-query';
import { AppState, Platform } from 'react-native';

import { isDerbyApiError } from '@/api/client';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60_000,
      gcTime: 30 * 60_000,
      // 4xx는 재시도 금지 (사용자 입력/상태 문제 — 재시도 무의미)
      retry: (count, err) => !isDerbyApiError(err, 4) && count < 2,
      refetchOnWindowFocus: false, // RN에선 AppState 기반으로 아래에서 직접 연결
    },
  },
});

/** AppState 'active' 복귀 → focusManager (TanStack RN 표준 패턴). 루트에서 1회 호출 */
export function wireAppStateFocus(): () => void {
  const sub = AppState.addEventListener('change', (status) => {
    if (Platform.OS !== 'web') {
      focusManager.setFocused(status === 'active');
    }
  });
  return () => sub.remove();
}
