import type * as Notifications from 'expo-notifications';
import { router } from 'expo-router';

import { openPushLog } from '@/api/endpoints/push';

/** 푸시 data payload → 앱 내 경로. deeplink 규약: dubby://home|tasks|diary|chat */
export function routeFromDeeplink(deeplink: string | undefined): string {
  switch ((deeplink ?? '').replace('dubby://', '')) {
    case 'tasks':
      return '/tasks';
    case 'diary':
      return '/diary';
    case 'chat':
      return '/chat';
    default:
      return '/';
  }
}

/** 알림 탭 공통 처리: 오픈 추적(fire-and-forget) + 경로 반환 */
export function handleNotificationResponse(
  response: Notifications.NotificationResponse,
): string {
  const data = response.notification.request.content.data as
    | { pushLogId?: number; deeplink?: string }
    | undefined;
  if (data?.pushLogId) {
    openPushLog(Number(data.pushLogId)).catch(() => {});
  }
  return routeFromDeeplink(data?.deeplink);
}

export function navigateTo(path: string) {
  router.push(path as never);
}
