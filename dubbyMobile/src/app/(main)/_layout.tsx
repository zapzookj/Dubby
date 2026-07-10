import { Redirect, Stack } from 'expo-router';

import { useAppStateStore } from '@/stores/appStateStore';
import { colors } from '@/theme/tokens';

export default function MainLayout() {
  const onboardingCompleted = useAppStateStore((s) => s.onboardingCompleted);

  if (!onboardingCompleted) {
    return <Redirect href="/onboarding" />;
  }

  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: colors.background },
        headerTintColor: colors.ink,
        headerTitleStyle: { fontWeight: '700' },
        headerShadowVisible: false,
        contentStyle: { backgroundColor: colors.background },
      }}>
      <Stack.Screen name="index" options={{ headerShown: false }} />
      <Stack.Screen name="tasks/index" options={{ title: '오늘의 업무 보고' }} />
      <Stack.Screen name="tasks/[assignmentId]" options={{ title: '업무 상세' }} />
      <Stack.Screen name="tasks/saved" options={{ title: '공식 흑역사 기록' }} />
      <Stack.Screen name="chat/index" options={{ title: '더비와 협의하기' }} />
      <Stack.Screen name="chat/exhausted" options={{ title: '더비 과로 안내' }} />
      <Stack.Screen name="diary/index" options={{ title: '더비의 일기장' }} />
      <Stack.Screen name="diary/[entryId]" options={{ title: '더비의 기억' }} />
      <Stack.Screen name="settings" options={{ title: '설정' }} />
    </Stack>
  );
}
