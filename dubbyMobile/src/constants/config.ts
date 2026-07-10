import { Platform } from 'react-native';

/**
 * API base URL.
 * - 실기기/운영: EXPO_PUBLIC_API_BASE_URL 환경변수
 * - Android 에뮬레이터: 호스트 localhost = 10.0.2.2
 * - iOS 시뮬레이터: localhost 그대로
 */
const devDefault = Platform.select({
  android: 'http://10.0.2.2:8080',
  default: 'http://localhost:8080',
});

export const API_BASE_URL =
  process.env.EXPO_PUBLIC_API_BASE_URL ?? devDefault ?? 'http://localhost:8080';

export const API_PREFIX = '/api/v1';
