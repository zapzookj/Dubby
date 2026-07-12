import Constants from 'expo-constants';
import * as Device from 'expo-device';
import { Platform } from 'react-native';

import { registerPushToken } from '@/api/endpoints/push';
import { handleNotificationResponse } from '@/notifications/router';

/**
 * ⚠ expo-notifications는 Expo Go(Android, SDK 53+)에서 **import 시점에 예외**를 던진다
 * (원격 푸시 기능이 Expo Go에서 제거됨). 따라서 이 모듈 어디에서도 정적 import 금지 —
 * 개발 빌드/실배포에서만 lazy require로 로드한다. Expo Go에서는 모든 함수가 조용히 no-op.
 */
const isExpoGo = Constants.appOwnership === 'expo';

type NotificationsModule = typeof import('expo-notifications');

let cached: NotificationsModule | null | undefined;

function getNotifications(): NotificationsModule | null {
  if (cached !== undefined) return cached;
  if (isExpoGo) {
    cached = null;
    return cached;
  }
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    cached = require('expo-notifications') as NotificationsModule;
  } catch {
    cached = null;
  }
  return cached;
}

let handlerSet = false;

/** 포그라운드 수신 시에도 배너 표시 (최초 1회) */
function ensureForegroundHandler(notifications: NotificationsModule): void {
  if (handlerSet) return;
  notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowBanner: true,
      shouldShowList: true,
      shouldPlaySound: false,
      shouldSetBadge: false,
    }),
  });
  handlerSet = true;
}

/** Android 채널 선행 생성 (권한 요청 전 필수) */
async function ensureAndroidChannel(notifications: NotificationsModule): Promise<void> {
  if (Platform.OS === 'android') {
    await notifications.setNotificationChannelAsync('default', {
      name: '더비의 알림',
      importance: notifications.AndroidImportance.DEFAULT,
    });
  }
}

/**
 * 알림 탭 라우팅 배선: 콜드 스타트(마지막 응답) + 실행 중 리스너.
 * 반환: 해제 함수. Expo Go에서는 no-op.
 */
export function wireNotificationRouting(onDeepLink: (path: string) => void): () => void {
  const notifications = getNotifications();
  if (!notifications) return () => {};
  ensureForegroundHandler(notifications);

  notifications.getLastNotificationResponseAsync().then((response) => {
    if (response) onDeepLink(handleNotificationResponse(response));
  }).catch(() => {});

  const sub = notifications.addNotificationResponseReceivedListener((response) => {
    onDeepLink(handleNotificationResponse(response));
  });
  return () => sub.remove();
}

/**
 * OS 권한 요청 → Expo 토큰 발급 → 서버 등록.
 * 반환: 'granted' | 'denied' | 'unavailable'(Expo Go/에뮬레이터/EAS projectId 없음 — 조용히 스킵)
 */
export async function requestAndRegisterPush(): Promise<'granted' | 'denied' | 'unavailable'> {
  const notifications = getNotifications();
  if (!notifications || !Device.isDevice) return 'unavailable';

  ensureForegroundHandler(notifications);
  await ensureAndroidChannel(notifications);

  const { status: existing } = await notifications.getPermissionsAsync();
  let status = existing;
  if (existing !== 'granted') {
    ({ status } = await notifications.requestPermissionsAsync());
  }
  if (status !== 'granted') return 'denied';

  const projectId: string | undefined =
    Constants.expoConfig?.extra?.eas?.projectId ?? Constants.easConfig?.projectId;
  if (!projectId) {
    // EAS 프로젝트 미생성 (외부 트랙) — 토큰 발급 불가. 권한만 확보하고 스킵
    return 'unavailable';
  }

  try {
    const token = (await notifications.getExpoPushTokenAsync({ projectId })).data;
    await registerPushToken(token, undefined, Platform.OS === 'ios' ? 'IOS' : 'ANDROID');
    return 'granted';
  } catch {
    return 'unavailable';
  }
}

/** 앱 시작 시 조용한 재등록 (토큰 로테이션 대응) — 권한 있을 때만 */
export async function silentReRegister(): Promise<void> {
  const notifications = getNotifications();
  if (!notifications) return;
  try {
    const { status } = await notifications.getPermissionsAsync();
    if (status === 'granted') {
      await requestAndRegisterPush();
    }
  } catch {
    // 무시 — 다음 실행에서 재시도
  }
}
