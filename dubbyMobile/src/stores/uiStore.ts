import { create } from 'zustand';

interface ToastPayload {
  id: number;
  message: string;
}

interface UiState {
  /** 이스터에그 세션당 1회 발동 기록 */
  easterEggFired: Record<string, boolean>;
  /** 콜드 스타트 푸시 딥링크 — 가드 통과 후 소비 */
  pendingDeepLink: string | null;
  toast: ToastPayload | null;
  fireEasterEgg: (key: string) => boolean;
  setPendingDeepLink: (path: string | null) => void;
  showToast: (message: string) => void;
  clearToast: () => void;
}

let toastSeq = 0;

export const useUiStore = create<UiState>((set, get) => ({
  easterEggFired: {},
  pendingDeepLink: null,
  toast: null,
  /** 반환값: 이번에 발동했는가 (false = 이미 발동됨 → 정상 동작 진행) */
  fireEasterEgg: (key) => {
    if (get().easterEggFired[key]) return false;
    set((s) => ({ easterEggFired: { ...s.easterEggFired, [key]: true } }));
    return true;
  },
  setPendingDeepLink: (path) => set({ pendingDeepLink: path }),
  showToast: (message) => set({ toast: { id: ++toastSeq, message } }),
  clearToast: () => set({ toast: null }),
}));
