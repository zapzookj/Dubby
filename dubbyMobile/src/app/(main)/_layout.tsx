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
    </Stack>
  );
}
