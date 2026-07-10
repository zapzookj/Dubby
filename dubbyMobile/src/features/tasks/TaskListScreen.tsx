import { router } from 'expo-router';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';

import { DerbyAvatar } from '@/components/DerbyAvatar';
import { DerbyErrorView } from '@/components/DerbyErrorView';
import { DerbyLoading } from '@/components/DerbyLoading';
import { JankyCard } from '@/components/JankyCard';
import { useTasksToday } from '@/features/tasks/useTasksToday';
import { emptyStates } from '@/theme/copy';
import { colors, spacing, typography } from '@/theme/tokens';

export default function TaskListScreen() {
  const tasks = useTasksToday();

  if (tasks.isPending) return <DerbyLoading scriptKey="tasks" />;
  if (tasks.isError) return <DerbyErrorView error={tasks.error} onRetry={() => tasks.refetch()} />;

  if (tasks.data.tasks.length === 0) {
    return (
      <View style={styles.empty}>
        <DerbyAvatar mood="idle" size={90} />
        <Text style={styles.emptyText}>{emptyStates.tasks}</Text>
      </View>
    );
  }

  return (
    <ScrollView style={styles.root} contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
      <Text style={styles.notice}>업무는 사용자님 기준 자정에 갱신됩니다.</Text>
      {tasks.data.tasks.map((task) => (
        <Pressable key={task.assignmentId} onPress={() => router.push(`/tasks/${task.assignmentId}`)}>
          <JankyCard seed={task.assignmentId} style={styles.card}>
            <View style={styles.cardHeader}>
              <Text style={styles.title}>✅ {task.title}</Text>
              {task.reaction ? (
                <Text style={styles.reactedBadge}>확인함</Text>
              ) : (
                <Text style={styles.newBadge}>NEW</Text>
              )}
            </View>
            <Text style={styles.conclusion} numberOfLines={2}>
              결론: {task.conclusion}
            </Text>
          </JankyCard>
        </Pressable>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.background },
  scroll: { padding: spacing(4), gap: spacing(4), paddingBottom: spacing(10) },
  notice: { ...typography.caption, color: colors.inkSub, textAlign: 'center' },
  card: { gap: spacing(2) },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', gap: spacing(2) },
  title: { ...typography.body, fontWeight: '700', color: colors.ink, flex: 1 },
  conclusion: { ...typography.body, color: colors.inkSub },
  newBadge: {
    ...typography.caption,
    color: colors.ink,
    backgroundColor: colors.tapeYellow,
    borderWidth: 1,
    borderColor: colors.border,
    paddingHorizontal: 6,
    paddingVertical: 1,
    borderRadius: 4,
    overflow: 'hidden',
    fontWeight: '700',
  },
  reactedBadge: { ...typography.caption, color: colors.praise, fontWeight: '700' },
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
