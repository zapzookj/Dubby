import AsyncStorage from '@react-native-async-storage/async-storage';
import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

interface AppState {
  onboardingCompleted: boolean;
  lastPushPromptAt: number | null;
  homeVisitCount: number;
  completeOnboarding: () => void;
  recordPushPrompt: () => void;
  recordHomeVisit: () => void;
}

/** 기기 로컬 영속 플래그 (서버 데이터 아님) */
export const useAppStateStore = create<AppState>()(
  persist(
    (set) => ({
      onboardingCompleted: false,
      lastPushPromptAt: null,
      homeVisitCount: 0,
      completeOnboarding: () => set({ onboardingCompleted: true }),
      recordPushPrompt: () => set({ lastPushPromptAt: Date.now() }),
      recordHomeVisit: () => set((s) => ({ homeVisitCount: s.homeVisitCount + 1 })),
    }),
    {
      name: 'dubby.appState',
      storage: createJSONStorage(() => AsyncStorage),
    },
  ),
);
