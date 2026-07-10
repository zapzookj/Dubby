import { useInfiniteQuery } from '@tanstack/react-query';
import { FlatList, StyleSheet, Text, View } from 'react-native';

import { getSavedTasks } from '@/api/endpoints/tasks';
import { queryKeys } from '@/api/queryKeys';
import { DerbyAvatar } from '@/components/DerbyAvatar';
import { DerbyErrorView } from '@/components/DerbyErrorView';
import { DerbyLoading } from '@/components/DerbyLoading';
import { JankyCard } from '@/components/JankyCard';
import { colors, spacing, typography } from '@/theme/tokens';

export default function SavedTasksScreen() {
  const saved = useInfiniteQuery({
    queryKey: queryKeys.tasksSaved,
    queryFn: ({ pageParam }) => getSavedTasks(pageParam),
    initialPageParam: null as number | null,
    getNextPageParam: (last) => (last.hasNext ? last.nextCursor : undefined),
    staleTime: 2 * 60_000,
  });

  if (saved.isPending) return <DerbyLoading scriptKey="tasks" />;
  if (saved.isError) return <DerbyErrorView error={saved.error} onRetry={() => saved.refetch()} />;

  const items = saved.data.pages.flatMap((p) => p.items);

  if (items.length === 0) {
    return (
      <View style={styles.empty}>
        <DerbyAvatar mood="idle" size={90} />
        <Text style={styles.emptyText}>
          아직 저장된 흑역사가 없습니다.{'\n'}더비가 곧 만들어드리겠습니다.
        </Text>
      </View>
    );
  }

  return (
    <FlatList
      style={styles.root}
      contentContainerStyle={styles.list}
      data={items}
      keyExtractor={(item) => String(item.assignmentId)}
      onEndReached={() => saved.hasNextPage && saved.fetchNextPage()}
      renderItem={({ item }) => (
        <JankyCard seed={item.assignmentId} style={styles.card}>
          <Text style={styles.date}>{item.assignedDate}</Text>
          <Text style={styles.title}>✅ {item.title}</Text>
          <Text style={styles.body}>결론: {item.conclusion}</Text>
          {item.note && <Text style={styles.note}>비고: {item.note}</Text>}
        </JankyCard>
      )}
    />
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.background },
  list: { padding: spacing(4), gap: spacing(4), paddingBottom: spacing(10) },
  card: { gap: spacing(1) },
  date: { ...typography.caption, color: colors.inkSub },
  title: { ...typography.body, fontWeight: '700', color: colors.ink },
  body: { ...typography.body, color: colors.ink },
  note: { ...typography.caption, color: colors.inkSub },
  empty: {
    flex: 1,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing(4),
    padding: spacing(6),
  },
  emptyText: { ...typography.body, color: colors.inkSub, textAlign: 'center' },
});
