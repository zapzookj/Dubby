import { QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { queryClient, wireAppStateFocus } from '@/api/queryClient';
import { DerbyErrorView } from '@/components/DerbyErrorView';
import { DerbyLoading } from '@/components/DerbyLoading';
import { DerbyToastHost } from '@/components/DerbyToast';
import { useAuthStore } from '@/stores/authStore';
import { useAppStateHydrated } from '@/stores/useAppStateHydrated';

SplashScreen.preventAutoHideAsync().catch(() => {});

export default function RootLayout() {
  const authStatus = useAuthStore((s) => s.status);
  const bootstrap = useAuthStore((s) => s.bootstrap);
  const hydrated = useAppStateHydrated();

  useEffect(() => {
    bootstrap();
    return wireAppStateFocus();
  }, [bootstrap]);

  useEffect(() => {
    if (authStatus !== 'loading' && hydrated) {
      SplashScreen.hideAsync().catch(() => {});
    }
  }, [authStatus, hydrated]);

  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <StatusBar style="dark" />
        <Gate authStatus={authStatus} hydrated={hydrated} onRetry={bootstrap} />
        <DerbyToastHost />
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}

function Gate({
  authStatus,
  hydrated,
  onRetry,
}: {
  authStatus: 'loading' | 'guest' | 'error';
  hydrated: boolean;
  onRetry: () => void;
}) {
  if (authStatus === 'loading' || !hydrated) {
    return <DerbyLoading />;
  }
  if (authStatus === 'error') {
    return <DerbyErrorView error={null} onRetry={onRetry} />;
  }
  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="(main)" />
      <Stack.Screen name="onboarding" options={{ animation: 'fade' }} />
    </Stack>
  );
}
