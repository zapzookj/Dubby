import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useLocalSearchParams } from 'expo-router';
import { Linking, ScrollView, StyleSheet, Text, View } from 'react-native';

import { getBillingMe } from '@/api/endpoints/billing';
import { queryKeys } from '@/api/queryKeys';
import { DerbyAvatar } from '@/components/DerbyAvatar';
import { DerbyLoading } from '@/components/DerbyLoading';
import { JankyButton } from '@/components/JankyButton';
import { JankyCard } from '@/components/JankyCard';
import {
  fetchOfferings, isPurchasesAvailable, purchase, restorePurchases,
} from '@/purchases/revenuecat';
import { useUiStore } from '@/stores/uiStore';
import { colors, radii, spacing, typography } from '@/theme/tokens';

/**
 * 더비 월급(Paywall) — 페르소나 §12.
 * 장난 카피와 가격/약관 영역을 시각적으로 분리 (가격 영역 = 정자세, 장난 금지 구역).
 * 가격은 스토어(RC 오퍼링)에서만 가져온다 — 하드코딩 가격 표기 금지.
 */
export default function SalaryScreen() {
  const { focus } = useLocalSearchParams<{ focus?: string }>();
  const queryClient = useQueryClient();
  const showToast = useUiStore((s) => s.showToast);

  const billing = useQuery({ queryKey: queryKeys.billing, queryFn: getBillingMe, staleTime: 60_000 });
  const offerings = useQuery({
    queryKey: ['purchases', 'offerings'],
    queryFn: fetchOfferings,
    staleTime: 5 * 60_000,
    enabled: isPurchasesAvailable(),
  });

  const invalidateAfterPurchase = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.billing });
    queryClient.invalidateQueries({ queryKey: queryKeys.home });
    queryClient.invalidateQueries({ queryKey: queryKeys.chatQuota });
  };

  const buy = useMutation({
    mutationFn: purchase,
    onSuccess: (completed) => {
      if (completed) {
        invalidateAfterPurchase();
        showToast('감사합니다. 더비가 월급 명세서를 액자에 넣었습니다.');
      }
    },
    onError: () => showToast('결제를 완료하지 못했습니다. 잠시 후 다시 시도해주세요.'),
  });

  const restore = useMutation({
    mutationFn: restorePurchases,
    onSuccess: () => {
      invalidateAfterPurchase();
      showToast('구매 내역을 확인했습니다.');
    },
    onError: () => showToast('구매 복원에 실패했습니다. 잠시 후 다시 시도해주세요.'),
  });

  if (billing.isPending) return <DerbyLoading />;

  const tier = billing.data?.tier ?? 'FREE';
  const available = isPurchasesAvailable();
  const products = offerings.data;

  return (
    <ScrollView style={styles.root} contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
      <View style={styles.header}>
        <DerbyAvatar mood={tier === 'SALARY' ? 'happy' : 'idle'} size={90} />
        <Text style={styles.pitch}>더비는 일을 잘하진 못하지만,{'\n'}출근은 잘 합니다.</Text>
        {tier === 'SALARY' && <Text style={styles.badge}>💰 월급 지급자</Text>}
      </View>

      {/* 더비 월급 (구독) */}
      <JankyCard seed="salary-product" style={styles.card}>
        <Text style={styles.productTitle}>더비 월급 (월 구독)</Text>
        <Text style={styles.benefit}>· 하루 채팅 {'​'}50회 (무료 3회)</Text>
        <Text style={styles.benefit}>· 일기장 슬롯 200개 (무료 30개)</Text>
        <Text style={styles.benefit}>· 업무 반응이 매번 새 문장 (더비가 진짜로 생각함)</Text>
        <Text style={styles.benefit}>· 일기 다시 쓰게 하기</Text>
        <View style={styles.priceBox}>
          {tier === 'SALARY' ? (
            <Text style={styles.priceInfo}>구독 중 · {billing.data?.willRenew ? '자동 갱신' : '갱신 안 함'}
              {billing.data?.expiresAt ? ` · ${billing.data.expiresAt.slice(0, 10)}까지` : ''}</Text>
          ) : products?.salary ? (
            <>
              <Text style={styles.price}>{products.salary.priceString} / 월</Text>
              <Text style={styles.priceInfo}>매월 자동 갱신 · 스토어 설정에서 언제든 해지 가능</Text>
              <JankyButton label="더비에게 월급 주기" seed="buy-salary"
                disabled={buy.isPending}
                onPress={() => buy.mutate(products.salary!)} />
            </>
          ) : (
            <Text style={styles.priceInfo}>
              {available ? '상품 정보를 불러오는 중입니다...' : '스토어 연결 준비 중입니다. 조금만 기다려주세요.'}
            </Text>
          )}
        </View>
      </JankyCard>

      {/* 커피 (소모성) */}
      <JankyCard seed="coffee-product" style={[styles.card, focus === 'coffee' && styles.focused]}>
        <Text style={styles.productTitle}>더비에게 커피 사주기</Text>
        <Text style={styles.benefit}>· 24시간 동안 하루 채팅 20회</Text>
        <Text style={styles.benefit}>· 더비가 각성한 척합니다 (실제 지능 상승 없음)</Text>
        <View style={styles.priceBox}>
          {products?.coffee ? (
            <>
              <Text style={styles.price}>{products.coffee.priceString} · 1회성</Text>
              <JankyButton label="커피 사주기" variant="secondary" seed="buy-coffee"
                disabled={buy.isPending}
                onPress={() => buy.mutate(products.coffee!)} />
            </>
          ) : (
            <Text style={styles.priceInfo}>
              {available ? '상품 정보를 불러오는 중입니다...' : '스토어 연결 준비 중입니다.'}
            </Text>
          )}
        </View>
      </JankyCard>

      {/* 장난 금지 구역: 복원/법적 고지 */}
      <View style={styles.legal}>
        <JankyButton label="구매 복원" variant="secondary" seed="restore"
          disabled={restore.isPending || !available}
          onPress={() => restore.mutate()} />
        <Text style={styles.legalText}>
          결제는 스토어 계정으로 청구됩니다. 구독은 기간 종료 24시간 전까지 해지하지 않으면 자동
          갱신됩니다. 해지는 스토어 구독 설정에서 가능합니다.
        </Text>
        <Text style={styles.legalLink} onPress={() => Linking.openURL('https://github.com/zapzookj/Dubby')}>
          이용약관 · 개인정보처리방침
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.background },
  scroll: { padding: spacing(4), gap: spacing(5), paddingBottom: spacing(12) },
  header: { alignItems: 'center', gap: spacing(3), marginTop: spacing(2) },
  pitch: { ...typography.title, color: colors.ink, textAlign: 'center' },
  badge: {
    ...typography.caption, fontWeight: '800', color: colors.ink,
    backgroundColor: colors.tapeYellow, borderWidth: 1, borderColor: colors.border,
    paddingHorizontal: spacing(3), paddingVertical: spacing(1), borderRadius: 6, overflow: 'hidden',
  },
  card: { gap: spacing(1) },
  focused: { borderColor: colors.derbyBlue, borderWidth: 3 },
  productTitle: { ...typography.title, fontSize: 17, color: colors.ink, marginBottom: spacing(1) },
  benefit: { ...typography.body, color: colors.inkSub },
  // 가격 영역 — 정자세 (장난 금지 신호: 기울기/장식 없음)
  priceBox: {
    marginTop: spacing(3),
    backgroundColor: colors.background,
    borderRadius: radii.button,
    padding: spacing(3),
    gap: spacing(2),
  },
  price: { fontSize: 18, fontWeight: '800', color: colors.ink },
  priceInfo: { ...typography.caption, color: colors.inkSub },
  legal: { gap: spacing(3), marginTop: spacing(2) },
  legalText: { ...typography.caption, color: colors.inkSub, lineHeight: 18 },
  legalLink: { ...typography.caption, color: colors.derbyBlue, textDecorationLine: 'underline' },
});
