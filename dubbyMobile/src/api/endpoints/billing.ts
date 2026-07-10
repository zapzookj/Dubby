import { request } from '@/api/client';
import type { Tier } from '@/api/types';

export interface BillingMe {
  tier: Tier;
  chatDailyLimit: number;
  expiresAt: string | null;
  willRenew: boolean;
  coffeeActiveUntil: string | null;
}

export function getBillingMe(): Promise<BillingMe> {
  return request<BillingMe>('/billing/me');
}

export function syncBilling(): Promise<BillingMe> {
  return request<BillingMe>('/billing/sync', { method: 'POST' });
}
