import { request } from '@/api/client';
import type { HomeResponse } from '@/api/types';

export function getHome(): Promise<HomeResponse> {
  return request<HomeResponse>('/home');
}
