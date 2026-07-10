import { QueryClientProvider } from '@tanstack/react-query';
import * as Notifications from 'expo-notifications';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { queryClient, wireAppStateFocus } from '@/api/queryClient';
import { DerbyErrorView } from '@/components/DerbyErrorView';
import { DerbyLoading } from '@/components/DerbyLoading';
import { DerbyToastHost } from '@/components/DerbyToast';
import { handleNotificationResponse } from '@/notifications/router';
import { silentReRegister } from '@/notifications/setup';
import { useAuthStore } from '@/stores/authStore';
import { useUiStore } from '@/stores/uiStore';
import { useAppStateHydrated } from '@/stores/useAppStateHydrated';

SplashScreen.preventAutoHideAsync().catch(() => {});

export default function RootLayout() {
  const authStatus = useAuthStore((s) => s.status);
  const bootstrap = useAuthStore((s) => s.bootstrap);
  const hydrated = useAppStateHydrated();
  const setPendingDeepLink = useUiStore((s) => s.setPendingDeepLink);

  useEffect(() => {
    bootstrap();
    return wireAppStateFocus();
  }, [bootstrap]);

  // 푸시: 토큰 재등록(멱등) + 탭 라우팅 (실행 중/콜드 스타트 양 경로)
  useEffect(() => {
    if (authStatus !== 'guest') return;
    silentReRegister();
    Notifications.getLastNotificationResponseAsync().then((response) => {
      if (response) setPendingDeepLink(handleNotificationResponse(response));
    });
    const sub = Notifications.addNotificationResponseReceivedListener((response) => {
      setPendingDeepLink(handleNotificationResponse(response));
    });
    return () => sub.remove();
  }, [authStatus, setPendingDeepLink]);

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
