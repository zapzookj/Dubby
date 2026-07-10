import { useQuery } from '@tanstack/react-query';
import { router } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';

import { getChatQuota } from '@/api/endpoints/chat';
import { queryKeys } from '@/api/queryKeys';
import { DerbyAvatar } from '@/components/DerbyAvatar';
import { JankyButton } from '@/components/JankyButton';
import { Screen } from '@/components/Screen';
import { useUiStore } from '@/stores/uiStore';
import { colors, spacing, typography } from '@/theme/tokens';

/** 채팅 제한(과로) 화면 — 결제 압박 금지: 커피 버튼 1개, 감정 조작 없음 */
export default function ExhaustedScreen() {
  const quota = useQuery({ queryKey: queryKeys.chatQuota, queryFn: getChatQuota, staleTime: 30_000 });
  const showToast = useUiStore((s) => s.showToast);

  const resetsAtLabel = quota.data?.resetsAt
    ? new Date(quota.data.resetsAt).toLocaleTimeString('ko-KR', { hour: 'numeric' })
    : '자정';

  return (
    <Screen>
      <View style={styles.center}>
        <DerbyAvatar mood="collapsed" size={120} />
        <Text style={styles.message}>
          {quota.data?.exhaustedMessage ?? '더비가 과로로 쓰러졌습니다.'}
        </Text>
        <Text style={styles.sub}>무료 더비는 하루에 생각을 많이 하면 위험합니다.</Text>
        <Text style={styles.reset}>내일 {resetsAtLabel}에 회복됩니다.</Text>
      </View>
      <View style={styles.footer}>
        <JankyButton
          label="더비에게 커피 사주기"
          seed="coffee"
          onPress={() => showToast('커피 결제는 준비 중입니다. 더비가 카드 단말기를 배우는 중입니다.')}
        />
        <JankyButton
          label="내일 다시 괴롭히기"
          variant="secondary"
          seed="tomorrow"
          onPress={() => router.replace('/')}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing(4), padding: spacing(4) },
  message: { ...typography.title, fontWeight: '600', color: colors.ink, textAlign: 'center' },
  sub: { ...typography.body, color: colors.inkSub, textAlign: 'center' },
  reset: { ...typography.caption, color: colors.derbyBlue, fontWeight: '700' },
  footer: { paddingBottom: spacing(8), gap: spacing(3) },
});
