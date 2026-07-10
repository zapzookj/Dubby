import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';
import Constants from 'expo-constants';
import { Platform } from 'react-native';

import { registerPushToken } from '@/api/endpoints/push';

// 포그라운드 수신 시에도 배너 표시
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: false,
    shouldSetBadge: false,
  }),
});

/** Android 채널 선행 생성 (권한 요청 전 필수) */
export async function ensureAndroidChannel(): Promise<void> {
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: '더비의 알림',
      importance: Notifications.AndroidImportance.DEFAULT,
    });
  }
}

/**
 * OS 권한 요청 → Expo 토큰 발급 → 서버 등록.
 * 반환: 'granted' | 'denied' | 'unavailable'(에뮬레이터/EAS projectId 없음 — 조용히 스킵)
 */
export async function requestAndRegisterPush(): Promise<'granted' | 'denied' | 'unavailable'> {
  if (!Device.isDevice) return 'unavailable'; // 시뮬레이터/에뮬레이터는 원격 푸시 불가

  await ensureAndroidChannel();
  const { status: existing } = await Notifications.getPermissionsAsync();
  let status = existing;
  if (existing !== 'granted') {
    ({ status } = await Notifications.requestPermissionsAsync());
  }
  if (status !== 'granted') return 'denied';

  const projectId: string | undefined =
    Constants.expoConfig?.extra?.eas?.projectId ?? Constants.easConfig?.projectId;
  if (!projectId) {
    // EAS 프로젝트 미생성 (외부 트랙) — 토큰 발급 불가. 권한만 확보하고 스킵
    return 'unavailable';
  }

  try {
    const token = (await Notifications.getExpoPushTokenAsync({ projectId })).data;
    await registerPushToken(token, undefined, Platform.OS === 'ios' ? 'IOS' : 'ANDROID');
    return 'granted';
  } catch {
    return 'unavailable';
  }
}

/** 앱 시작 시 조용한 재등록 (토큰 로테이션 대응) — 권한 있을 때만 */
export async function silentReRegister(): Promise<void> {
  try {
    const { status } = await Notifications.getPermissionsAsync();
    if (status === 'granted') {
      await requestAndRegisterPush();
    }
  } catch {
    // 무시 — 다음 실행에서 재시도
  }
}
