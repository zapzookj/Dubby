import { useEffect, useState } from 'react';

import { useAppStateStore } from '@/stores/appStateStore';

/** zustand persist(AsyncStorage) 하이드레이션 완료 여부 — 완료 전 온보딩 리다이렉트 판단 금지 */
export function useAppStateHydrated(): boolean {
  const [hydrated, setHydrated] = useState(useAppStateStore.persist.hasHydrated());

  useEffect(() => {
    const unsub = useAppStateStore.persist.onFinishHydration(() => setHydrated(true));
    return unsub;
  }, []);

  return hydrated;
}
