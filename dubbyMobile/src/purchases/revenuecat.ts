import Constants from 'expo-constants';
import { Platform } from 'react-native';

import { syncBilling } from '@/api/endpoints/billing';

/**
 * RevenueCat 연동 (P4).
 * - appUserID = 서버 발급 userId(UUID) — 웹훅 app_user_id와 1:1
 * - Expo Go / RC 키 미설정 환경에서는 네이티브 호출을 건너뛴다 (개발 편의 가드)
 * - usePremium류 UI 게이팅만 클라 담당. 실제 기능 제한은 항상 서버 판정
 *
 * RC public SDK key는 EXPO_PUBLIC_RC_ANDROID_KEY / EXPO_PUBLIC_RC_IOS_KEY 환경변수로 주입
 * (public key라 클라 노출 허용 — 시크릿 아님)
 */

const RC_KEY = Platform.select({
  ios: process.env.EXPO_PUBLIC_RC_IOS_KEY,
  android: process.env.EXPO_PUBLIC_RC_ANDROID_KEY,
});

/** Expo Go에서는 네이티브 모듈 없음 → 결제 비활성 */
export function isPurchasesAvailable(): boolean {
  return Boolean(RC_KEY) && Constants.appOwnership !== 'expo';
}

let configured = false;

// 타입만 정적 참조 (런타임 로드는 가드 뒤에서)
type PurchasesModule = typeof import('react-native-purchases').default;

function loadPurchases(): PurchasesModule | null {
  if (!isPurchasesAvailable()) return null;
  try {
    // eslint-disable-next-line @typescript-eslint/no-require-imports
    return require('react-native-purchases').default as PurchasesModule;
  } catch {
    return null;
  }
}

/** 인증 완료 후 1회 호출 */
export async function configurePurchases(userId: string): Promise<void> {
  const Purchases = loadPurchases();
  if (!Purchases || configured) return;
  Purchases.configure({ apiKey: RC_KEY as string, appUserID: userId });
  configured = true;
}

export interface OfferingProduct {
  identifier: string;
  title: string;
  priceString: string;
  packageObject: unknown;
}

export interface DubbyOfferings {
  salary: OfferingProduct | null;
  coffee: OfferingProduct | null;
}

/** 현재 오퍼링 → 더비 상품 매핑. 미연결 시 null (Paywall은 준비 중 표시) */
export async function fetchOfferings(): Promise<DubbyOfferings | null> {
  const Purchases = loadPurchases();
  if (!Purchases) return null;
  try {
    const offerings = await Purchases.getOfferings();
    const current = offerings.current;
    if (!current) return { salary: null, coffee: null };
    const find = (productPrefix: string): OfferingProduct | null => {
      const pkg = current.availablePackages.find((p) =>
        p.product.identifier.startsWith(productPrefix));
      return pkg
        ? {
            identifier: pkg.product.identifier,
            title: pkg.product.title,
            priceString: pkg.product.priceString,
            packageObject: pkg,
          }
        : null;
    };
    return { salary: find('dubby_salary'), coffee: find('dubby_coffee') };
  } catch {
    return null;
  }
}

/** 구매 → 서버 동기화(웹훅 지연 대비). 성공 시 true, 사용자 취소 시 false, 그 외 throw */
export async function purchase(product: OfferingProduct): Promise<boolean> {
  const Purchases = loadPurchases();
  if (!Purchases) return false;
  try {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    await Purchases.purchasePackage(product.packageObject as any);
    await syncBilling().catch(() => {});
    return true;
  } catch (e) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    if ((e as any)?.userCancelled) return false;
    throw e;
  }
}

export async function restorePurchases(): Promise<void> {
  const Purchases = loadPurchases();
  if (!Purchases) return;
  await Purchases.restorePurchases();
  await syncBilling().catch(() => {});
}
