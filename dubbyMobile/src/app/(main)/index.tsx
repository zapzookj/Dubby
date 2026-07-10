import { useQuery } from '@tanstack/react-query';
import { StyleSheet, Text, View } from 'react-native';

import { request } from '@/api/client';
import { DerbyAvatar } from '@/components/DerbyAvatar';
import { DerbyErrorView } from '@/components/DerbyErrorView';
import { DerbyLoading } from '@/components/DerbyLoading';
import { JankyCard } from '@/components/JankyCard';
import { Screen } from '@/components/Screen';
import { useAuthStore } from '@/stores/authStore';
import { colors, spacing, typography } from '@/theme/tokens';

/**
 * 홈 (P0 임시판) — 서버 연결 검증용.
 * P1에서 GET /home 기반 DerbyStatusCard + MenuGrid로 교체된다.
 */
export default function HomeScreen() {
  const userId = useAuthStore((s) => s.userId);

  const health = useQuery({
    queryKey: ['health'],
    queryFn: () => request<{ status: string; derbyMessage: string }>('/health'),
  });

  if (health.isPending) {
    return <DerbyLoading />;
  }
  if (health.isError) {
    return <DerbyErrorView error={health.error} onRetry={() => health.refetch()} />;
  }

  return (
    <Screen>
      <View style={styles.center}>
        <DerbyAvatar mood="confident" size={110} />
        <JankyCard seed="home-status" style={styles.card}>
          <Text style={styles.statusTitle}>오늘도 대충 준비됨</Text>
          <Text style={styles.statusLine}>{health.data.derbyMessage}</Text>
          <Text style={styles.caption}>사번: {userId ?? '발급 중'}</Text>
        </JankyCard>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing(6),
  },
  card: {
    alignSelf: 'stretch',
    gap: spacing(2),
  },
  statusTitle: {
    ...typography.title,
    color: colors.ink,
  },
  statusLine: {
    ...typography.body,
    color: colors.inkSub,
  },
  caption: {
    ...typography.caption,
    color: colors.inkSub,
  },
});
