import { create } from 'zustand';

import { authenticateDevice } from '@/api/client';

type AuthStatus = 'loading' | 'guest' | 'error';

interface AuthState {
  status: AuthStatus;
  userId: string | null;
  /** bootstrap 실패 원인 — Gate의 DerbyErrorView가 실제 에러(네트워크 등)를 표시하는 데 사용 */
  error: unknown;
  bootstrap: () => Promise<void>;
}

/** 인증 라이프사이클만 담당. 토큰 원본은 SecureStore, 서버 데이터는 Query 캐시 */
export const useAuthStore = create<AuthState>((set) => ({
  status: 'loading',
  userId: null,
  error: null,
  bootstrap: async () => {
    set({ status: 'loading', error: null });
    try {
      const auth = await authenticateDevice();
      set({ status: 'guest', userId: auth.userId });
    } catch (e) {
      set({ status: 'error', userId: null, error: e });
    }
  },
}));
