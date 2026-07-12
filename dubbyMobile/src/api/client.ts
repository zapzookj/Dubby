import * as SecureStore from 'expo-secure-store';
import * as ExpoCrypto from 'expo-crypto';
import * as Localization from 'expo-localization';
import * as Application from 'expo-application';
import { Platform } from 'react-native';

import { API_BASE_URL, API_PREFIX } from '@/constants/config';
import { ErrorCodes } from '@/api/errorCodes';
import { networkErrorCopy } from '@/theme/copy';
import type { ApiErrorBody, DeviceAuthResponse } from '@/api/types';

const KEY_ACCESS_TOKEN = 'dubby.accessToken';
const KEY_DEVICE_ID = 'dubby.deviceId';

export class DerbyApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly derbyMessage: string;
  readonly body: ApiErrorBody | null;

  constructor(status: number, body: ApiErrorBody | null) {
    super(body?.message ?? `HTTP ${status}`);
    this.status = status;
    this.code = body?.code ?? (status === 0 ? ErrorCodes.NETWORK : ErrorCodes.COMMON_INTERNAL_ERROR);
    this.derbyMessage =
      body?.derbyMessage ??
      networkErrorCopy[Math.floor(Math.random() * networkErrorCopy.length)];
    this.body = body;
  }
}

export function isDerbyApiError(e: unknown, statusFamily?: number): e is DerbyApiError {
  if (!(e instanceof DerbyApiError)) return false;
  return statusFamily === undefined || Math.floor(e.status / 100) === statusFamily;
}

// ── deviceId: 게스트 계정의 앵커 (최초 실행 시 생성, 영구 보관) ──

async function getOrCreateDeviceId(): Promise<string> {
  let id = await SecureStore.getItemAsync(KEY_DEVICE_ID);
  if (!id) {
    id = generateUuid();
    await SecureStore.setItemAsync(KEY_DEVICE_ID, id);
  }
  return id;
}

export function generateUuid(): string {
  // ⚠ RN(Hermes)에는 웹 표준 crypto.getRandomValues가 없다 — expo-crypto의 CSPRNG 사용
  // (deviceId는 사실상 계정 자격증명이므로 Math.random 폴백 금지)
  return ExpoCrypto.randomUUID();
}

// ── 게스트 인증 (401 시 동일 deviceId 재호출 — refresh 토큰 없음) ──

let authInFlight: Promise<DeviceAuthResponse> | null = null;

export async function authenticateDevice(): Promise<DeviceAuthResponse> {
  // 뮤텍스: 동시 401 다발 시 인증 1회만
  if (!authInFlight) {
    authInFlight = (async () => {
      try {
        const deviceId = await getOrCreateDeviceId();
        const res = await fetch(`${API_BASE_URL}${API_PREFIX}/auth/device`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            deviceId,
            platform: Platform.OS === 'ios' ? 'IOS' : 'ANDROID',
            timezone: Localization.getCalendars()[0]?.timeZone ?? 'Asia/Seoul',
            locale: (Localization.getLocales()[0]?.languageTag ?? 'ko-KR').slice(0, 10),
            appVersion: Application.nativeApplicationVersion ?? undefined,
          }),
        });
        if (!res.ok) {
          throw new DerbyApiError(res.status, await safeJson(res));
        }
        const data = (await res.json()) as DeviceAuthResponse;
        await SecureStore.setItemAsync(KEY_ACCESS_TOKEN, data.accessToken);
        return data;
      } finally {
        authInFlight = null;
      }
    })();
  }
  return authInFlight;
}

async function safeJson(res: Response): Promise<ApiErrorBody | null> {
  try {
    return (await res.json()) as ApiErrorBody;
  } catch {
    return null;
  }
}

// ── 공용 요청 함수 ──

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = await SecureStore.getItemAsync(KEY_ACCESS_TOKEN);
  const doFetch = (accessToken: string | null) =>
    fetch(`${API_BASE_URL}${API_PREFIX}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        ...(init?.headers ?? {}),
      },
    });

  let res: Response;
  try {
    res = await doFetch(token);
  } catch {
    throw new DerbyApiError(0, null); // 네트워크 단절
  }

  // 401 → 게스트 재인증 1회 후 원 요청 재시도 (동일 deviceId = 동일 계정)
  if (res.status === 401) {
    const auth = await authenticateDevice();
    try {
      res = await doFetch(auth.accessToken);
    } catch {
      throw new DerbyApiError(0, null);
    }
  }

  if (!res.ok) {
    throw new DerbyApiError(res.status, await safeJson(res));
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return (await res.json()) as T;
}
