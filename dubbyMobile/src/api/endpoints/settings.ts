import { request } from '@/api/client';
import type { SettingsPatchRequest, SettingsResponse } from '@/api/types';

export function getSettings(): Promise<SettingsResponse> {
  return request<SettingsResponse>('/settings');
}

export function patchSettings(patch: SettingsPatchRequest): Promise<SettingsResponse> {
  return request<SettingsResponse>('/settings', {
    method: 'PATCH',
    body: JSON.stringify(patch),
  });
}

export function deleteAccount(): Promise<{ deleted: boolean; message: string }> {
  return request('/users/me', { method: 'DELETE' });
}
