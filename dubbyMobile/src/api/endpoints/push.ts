import { request } from '@/api/client';

export interface PushSettings {
  enabled: boolean;
  maxDailyCount: number;
}

export function registerPushToken(expoPushToken: string, deviceId?: string, platform?: string) {
  return request<{ registered: boolean }>('/push/tokens', {
    method: 'POST',
    body: JSON.stringify({ expoPushToken, deviceId, platform }),
  });
}

export function deletePushToken(expoPushToken: string) {
  return request<{ deleted: boolean }>('/push/tokens/delete', {
    method: 'POST',
    body: JSON.stringify({ expoPushToken }),
  });
}

export function getPushSettings(): Promise<PushSettings> {
  return request<PushSettings>('/push/settings');
}

export function putPushSettings(settings: Partial<PushSettings>): Promise<PushSettings> {
  return request<PushSettings>('/push/settings', {
    method: 'PUT',
    body: JSON.stringify(settings),
  });
}

export function openPushLog(pushLogId: number) {
  return request<{ opened: boolean }>(`/push/logs/${pushLogId}/open`, { method: 'POST' });
}
