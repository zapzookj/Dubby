import { create } from 'zustand';

import { authenticateDevice } from '@/api/client';

type AuthStatus = 'loading' | 'guest' | 'error';

interface AuthState {
  status: AuthStatus;
  userId: string | null;
  bootstrap: () => Promise<void>;
}

/** 인증 라이프사이클만 담당. 토큰 원본은 SecureStore, 서버 데이터는 Query 캐시 */
export const useAuthStore = create<AuthState>((set) => ({
  status: 'loading',
  userId: null,
  bootstrap: async () => {
    set({ status: 'loading' });
    try {
      const auth = await authenticateDevice();
      set({ status: 'guest', userId: auth.userId });
    } catch {
      set({ status: 'error', userId: null });
    }
  },
}));
