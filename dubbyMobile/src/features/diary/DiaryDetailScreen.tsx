import { useMutation, useQueryClient } from '@tanstack/react-query';
import { router, useLocalSearchParams } from 'expo-router';
import { Alert, ScrollView, Share, StyleSheet, Text } from 'react-native';

import { DerbyApiError } from '@/api/client';
import { deleteDiaryEntry, shareDiaryEntry } from '@/api/endpoints/diary';
import { queryKeys } from '@/api/queryKeys';
import type { CursorPage, DiaryEntry } from '@/api/types';
import { DerbyErrorView } from '@/components/DerbyErrorView';
import { JankyButton } from '@/components/JankyButton';
import { JankyCard } from '@/components/JankyCard';
import { useUiStore } from '@/stores/uiStore';
import { colors, spacing, typography } from '@/theme/tokens';

export default function DiaryDetailScreen() {
  const { entryId } = useLocalSearchParams<{ entryId: string }>();
  const id = Number(entryId);
  const queryClient = useQueryClient();
  const showToast = useUiStore((s) => s.showToast);

  // 리스트 캐시에서 즉시 렌더 (추가 GET 불필요)
  const cached = queryClient.getQueryData<{ pages: CursorPage<DiaryEntry>[] }>(queryKeys.diary);
  const entry = cached?.pages.flatMap((p) => p.items).find((e) => e.entryId === id);

  const del = useMutation({
    mutationFn: () => deleteDiaryEntry(id),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.diary });
      showToast(res.derbyMessage);
      router.back();
    },
    onError: (e) => e instanceof DerbyApiError && showToast(e.derbyMessage),
  });

  const share = useMutation({
    mutationFn: () => shareDiaryEntry(id),
    onSuccess: async ({ shareText }) => {
      await Share.share({ message: shareText });
    },
  });

  if (!entry) {
    return <DerbyErrorView error={null} onRetry={() => router.back()} />;
  }

  const confirmDelete = () =>
    Alert.alert('일기 삭제', '이 기억을 삭제합니다. 삭제하면 복구할 수 없습니다.', [
      { text: '취소', style: 'cancel' },
      { text: '삭제', style: 'destructive', onPress: () => del.mutate() },
    ]);

  return (
    <ScrollView style={styles.root} contentContainerStyle={styles.scroll}>
      <JankyCard seed={entry.entryId} style={styles.card}>
        <Text style={styles.date}>{entry.createdAt.slice(0, 10)} {entry.autoSaved ? '· 더비가 몰래 적음' : ''}</Text>
        <Text style={styles.fact}>{entry.fact}</Text>
        <Text style={styles.body}>{entry.interpretation}</Text>
        <Text style={styles.conclusion}>{entry.conclusion}</Text>
      </JankyCard>
      <JankyButton label="공유하기" seed="diary-share" onPress={() => share.mutate()} />
      <JankyButton label="이 기억 삭제" variant="danger" seed="diary-delete"
        disabled={del.isPending} onPress={confirmDelete} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.background },
  scroll: { padding: spacing(4), gap: spacing(4), paddingBottom: spacing(10) },
  card: { gap: spacing(2) },
  date: { ...typography.caption, color: colors.inkSub },
  fact: { ...typography.title, fontWeight: '600', color: colors.ink },
  body: { ...typography.body, color: colors.ink },
  conclusion: { ...typography.body, fontWeight: '700', color: colors.derbyBlue },
});
