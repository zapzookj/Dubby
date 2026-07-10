import { useInfiniteQuery } from '@tanstack/react-query';
import { router } from 'expo-router';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { getDiaryEntries } from '@/api/endpoints/diary';
import { queryKeys } from '@/api/queryKeys';
import { DerbyAvatar } from '@/components/DerbyAvatar';
import { DerbyErrorView } from '@/components/DerbyErrorView';
import { DerbyLoading } from '@/components/DerbyLoading';
import { JankyCard } from '@/components/JankyCard';
import { emptyStates } from '@/theme/copy';
import { colors, spacing, typography } from '@/theme/tokens';

export default function DiaryListScreen() {
  const diary = useInfiniteQuery({
    queryKey: queryKeys.diary,
    queryFn: ({ pageParam }) => getDiaryEntries(pageParam),
    initialPageParam: null as number | null,
    getNextPageParam: (last) => (last.hasNext ? last.nextCursor : undefined),
    staleTime: 2 * 60_000,
  });

  if (diary.isPending) return <DerbyLoading />;
  if (diary.isError) return <DerbyErrorView error={diary.error} onRetry={() => diary.refetch()} />;

  const items = diary.data.pages.flatMap((p) => p.items);

  if (items.length === 0) {
    return (
      <View style={styles.empty}>
        <DerbyAvatar mood="idle" size={90} />
        <Text style={styles.emptyText}>{emptyStates.diary}</Text>
        <Text style={styles.emptyHint}>채팅에서 더비에게 사용자님 이야기를 들려주면{'\n'}더비가 이상하게 받아 적습니다.</Text>
      </View>
    );
  }

  return (
    <FlatList
      style={styles.root}
      contentContainerStyle={styles.list}
      data={items}
      keyExtractor={(item) => String(item.entryId)}
      onEndReached={() => diary.hasNextPage && diary.fetchNextPage()}
      renderItem={({ item }) => (
        <Pressable onPress={() => router.push(`/diary/${item.entryId}`)}>
          <JankyCard seed={item.entryId} style={styles.card}>
            <Text style={styles.date}>{item.createdAt.slice(0, 10)}</Text>
            <Text style={styles.fact}>{item.fact}</Text>
            <Text style={styles.interpretation} numberOfLines={2}>{item.interpretation}</Text>
          </JankyCard>
        </Pressable>
      )}
    />
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.background },
  list: { padding: spacing(4), gap: spacing(4), paddingBottom: spacing(10) },
  card: { gap: spacing(1) },
  date: { ...typography.caption, color: colors.inkSub },
  fact: { ...typography.body, fontWeight: '700', color: colors.ink },
  interpretation: { ...typography.body, color: colors.inkSub },
  empty: {
    flex: 1, backgroundColor: colors.background,
    alignItems: 'center', justifyContent: 'center', gap: spacing(4), padding: spacing(6),
  },
  emptyText: { ...typography.body, color: colors.ink, textAlign: 'center' },
  emptyHint: { ...typography.caption, color: colors.inkSub, textAlign: 'center' },
});
