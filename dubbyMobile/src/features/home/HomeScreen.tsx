import { useQuery } from '@tanstack/react-query';
import { router } from 'expo-router';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { getHome } from '@/api/endpoints/home';
import { queryKeys } from '@/api/queryKeys';
import { DerbyAvatar } from '@/components/DerbyAvatar';
import { DerbyErrorView } from '@/components/DerbyErrorView';
import { DerbyLoading } from '@/components/DerbyLoading';
import { JankyCard } from '@/components/JankyCard';
import { Screen } from '@/components/Screen';
import { useUiStore } from '@/stores/uiStore';
import { colors, jankyTilt, radii, spacing, typography } from '@/theme/tokens';

export default function HomeScreen() {
  const home = useQuery({ queryKey: queryKeys.home, queryFn: getHome, staleTime: 60_000 });
  const showToast = useUiStore((s) => s.showToast);

  if (home.isPending) return <DerbyLoading />;
  if (home.isError) return <DerbyErrorView error={home.error} onRetry={() => home.refetch()} />;

  const { derby, todayTasks, chatQuota, diary } = home.data;
  const unreacted = todayTasks.total - todayTasks.reactedCount;

  const comingSoon = () =>
    showToast('준비 중입니다. 더비가 아직 그 기능을 배우는 중입니다.');

  return (
    <Screen>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scroll}>
        <View style={styles.avatarRow}>
          <DerbyAvatar mood={derby.mood} size={110} />
        </View>

        <JankyCard seed="home-status" style={styles.statusCard}>
          <Text style={styles.statusTitle}>오늘의 더비</Text>
          <StatusRow label="상태" value={derby.statusLine} />
          <StatusRow label="정확도" value={derby.accuracy} />
          <StatusRow label="현재 업무" value={derby.currentWork} />
        </JankyCard>

        <View style={styles.grid}>
          <MenuTile
            seed="menu-tasks"
            emoji="📋"
            title="오늘의 업무"
            badge={unreacted > 0 ? `${unreacted}` : undefined}
            subtitle={`${todayTasks.reactedCount}/${todayTasks.total} 확인함`}
            onPress={() => router.push('/tasks')}
          />
          <MenuTile
            seed="menu-chat"
            emoji="💬"
            title="더비와 협의"
            subtitle={`남은 협의 ${chatQuota.remaining}회`}
            onPress={() => router.push(chatQuota.remaining === 0 ? '/chat/exhausted' : '/chat')}
          />
          <MenuTile
            seed="menu-diary"
            emoji="📔"
            title="더비의 일기장"
            badge={diary.pendingCandidates > 0 ? `${diary.pendingCandidates}` : undefined}
            subtitle={diary.totalEntries > 0 ? `${diary.totalEntries}개의 기억` : '아직 백지'}
            onPress={() => router.push('/diary')}
          />
          <MenuTile
            seed="menu-saved"
            emoji="🗃️"
            title="저장한 업무"
            subtitle="공식 흑역사"
            onPress={() => router.push('/tasks/saved')}
          />
          <MenuTile
            seed="menu-salary"
            emoji="💰"
            title="더비 월급"
            subtitle={home.data.billing.tier === 'SALARY' ? '월급 지급자 💰' : '출근은 잘 합니다'}
            onPress={() => router.push('/salary')}
          />
          <MenuTile
            seed="menu-settings"
            emoji="⚙️"
            title="설정"
            subtitle="대체로 작동함"
            onPress={() => router.push('/settings')}
          />
        </View>
      </ScrollView>
    </Screen>
  );
}

function StatusRow({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.statusRow}>
      <Text style={styles.statusLabel}>{label}</Text>
      <Text style={styles.statusValue}>{value}</Text>
    </View>
  );
}

function MenuTile({
  seed,
  emoji,
  title,
  subtitle,
  badge,
  onPress,
}: {
  seed: string;
  emoji: string;
  title: string;
  subtitle?: string;
  badge?: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [
        styles.tile,
        { transform: [{ rotate: `${jankyTilt(seed, 0.8)}deg` }] },
        pressed && styles.tilePressed,
      ]}>
      {badge && (
        <View style={styles.badge}>
          <Text style={styles.badgeText}>{badge}</Text>
        </View>
      )}
      <Text style={styles.tileEmoji}>{emoji}</Text>
      <Text style={styles.tileTitle}>{title}</Text>
      {subtitle && <Text style={styles.tileSubtitle}>{subtitle}</Text>}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingBottom: spacing(10), gap: spacing(5) },
  avatarRow: { alignItems: 'center', marginTop: spacing(2) },
  statusCard: { gap: spacing(2) },
  statusTitle: { ...typography.title, color: colors.ink, marginBottom: spacing(1) },
  statusRow: { flexDirection: 'row', justifyContent: 'space-between' },
  statusLabel: { ...typography.body, color: colors.inkSub },
  statusValue: { ...typography.body, color: colors.ink, fontWeight: '600', flexShrink: 1, textAlign: 'right' },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing(3) },
  tile: {
    width: '47.5%',
    backgroundColor: colors.surface,
    borderWidth: 2,
    borderColor: colors.border,
    borderRadius: radii.card,
    padding: spacing(4),
    gap: spacing(1),
    shadowColor: colors.border,
    shadowOpacity: 1,
    shadowRadius: 0,
    shadowOffset: { width: 3, height: 3 },
    elevation: 2,
  },
  tilePressed: { shadowOffset: { width: 1, height: 1 }, transform: [{ translateX: 2 }, { translateY: 2 }] },
  tileEmoji: { fontSize: 26 },
  tileTitle: { ...typography.body, fontWeight: '700', color: colors.ink },
  tileSubtitle: { ...typography.caption, color: colors.inkSub },
  badge: {
    position: 'absolute',
    top: -8,
    right: -6,
    backgroundColor: colors.accident,
    borderWidth: 2,
    borderColor: colors.border,
    borderRadius: 12,
    minWidth: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1,
  },
  badgeText: { color: '#FFF', fontSize: 12, fontWeight: '700' },
});
