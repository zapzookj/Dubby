import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocalSearchParams } from 'expo-router';
import { useRef, useState } from 'react';
import { ScrollView, Share, StyleSheet, Text, View } from 'react-native';
import ViewShot from 'react-native-view-shot';
import * as Sharing from 'expo-sharing';

import { DerbyApiError } from '@/api/client';
import { reactToTask, saveTask, shareTask } from '@/api/endpoints/tasks';
import { queryKeys } from '@/api/queryKeys';
import type { Reaction } from '@/api/types';
import { DerbyErrorView } from '@/components/DerbyErrorView';
import { DerbyLoading } from '@/components/DerbyLoading';
import { JankyButton } from '@/components/JankyButton';
import { JankyCard } from '@/components/JankyCard';
import { ShareCard } from '@/components/ShareCard';
import { useTasksToday, useUpdateTaskInCache } from '@/features/tasks/useTasksToday';
import { useUiStore } from '@/stores/uiStore';
import { colors, spacing, typography } from '@/theme/tokens';

export default function TaskDetailScreen() {
  const { assignmentId } = useLocalSearchParams<{ assignmentId: string }>();
  const id = Number(assignmentId);
  const tasks = useTasksToday();
  const updateCache = useUpdateTaskInCache();
  const queryClient = useQueryClient();
  const showToast = useUiStore((s) => s.showToast);

  const [followUp, setFollowUp] = useState<string | null>(null);
  const shareRef = useRef<React.ElementRef<typeof ViewShot>>(null);

  const reaction = useMutation({
    mutationFn: (r: Reaction) => reactToTask(id, r),
    onSuccess: (res) => {
      setFollowUp(res.followUpMessage);
      updateCache(id, { reaction: res.reaction });
      queryClient.invalidateQueries({ queryKey: queryKeys.home });
    },
    onError: (e) => {
      // RETRY 소진(409)은 고정 문구를 후속 반응처럼 표시 — 에러가 아니라 개그
      if (e instanceof DerbyApiError && e.code === 'TASK_RETRY_EXHAUSTED') {
        setFollowUp(e.derbyMessage);
      } else if (e instanceof DerbyApiError) {
        showToast(e.derbyMessage);
      }
    },
  });

  const save = useMutation({
    mutationFn: (saved: boolean) => saveTask(id, saved),
    onSuccess: (res) => {
      updateCache(id, { saved: res.saved });
      showToast(res.derbyMessage);
    },
    onError: (e) => e instanceof DerbyApiError && showToast(e.derbyMessage),
  });

  const share = useMutation({
    mutationFn: async () => {
      const { shareText } = await shareTask(id);
      // 이미지 캡처 공유 → 실패 시 텍스트 공유 폴백
      try {
        const uri = await shareRef.current?.capture?.();
        if (uri && (await Sharing.isAvailableAsync())) {
          await Sharing.shareAsync(uri, { mimeType: 'image/png' });
          return;
        }
      } catch {
        // fall through to text share
      }
      await Share.share({ message: shareText });
    },
    onError: (e) => e instanceof DerbyApiError && showToast(e.derbyMessage),
  });

  if (tasks.isPending) return <DerbyLoading scriptKey="tasks" />;
  if (tasks.isError) return <DerbyErrorView error={tasks.error} onRetry={() => tasks.refetch()} />;

  const task = tasks.data.tasks.find((t) => t.assignmentId === id);
  if (!task) {
    return <DerbyErrorView error={new DerbyApiError(404, {
      code: 'TASK_ASSIGNMENT_NOT_FOUND', message: 'not found',
      derbyMessage: '그 업무는 더비의 책상에 없습니다. 애초에 책상이 있었는지도 확인 중입니다.',
    })} />;
  }

  return (
    <ScrollView style={styles.root} contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
      <JankyCard seed={task.assignmentId} style={styles.report}>
        <Text style={styles.title}>✅ {task.title}</Text>
        <View style={styles.divider} />
        <Text style={styles.label}>결론</Text>
        <Text style={styles.body}>{task.conclusion}</Text>
        {task.note && (
          <>
            <Text style={styles.label}>비고</Text>
            <Text style={styles.body}>{task.note}</Text>
          </>
        )}
      </JankyCard>

      {followUp && (
        <View style={styles.followUpRow}>
          <Text style={styles.followUpEmoji}>🤖</Text>
          <View style={styles.followUpBubble}>
            <Text style={styles.body}>{followUp}</Text>
          </View>
        </View>
      )}

      <View style={styles.reactions}>
        {task.buttons.map((b) => (
          <JankyButton
            key={b.key}
            seed={`${task.assignmentId}-${b.key}`}
            label={b.label}
            variant={task.reaction === b.key ? 'primary' : 'secondary'}
            disabled={reaction.isPending}
            onPress={() => reaction.mutate(b.key)}
            style={styles.reactionButton}
          />
        ))}
      </View>
      {reaction.isPending && <DerbyLoading variant="mini" scriptKey="tasks" />}

      <View style={styles.bottomRow}>
        <JankyButton
          seed="save"
          label={task.saved ? '저장됨 ✓' : '도감 저장'}
          variant="secondary"
          disabled={save.isPending}
          onPress={() => save.mutate(!task.saved)}
          style={styles.flex1}
        />
        <JankyButton
          seed="share"
          label="공유하기"
          disabled={share.isPending}
          onPress={() => share.mutate()}
          style={styles.flex1}
        />
      </View>

      {/* 오프스크린 공유 카드 (캡처 전용) */}
      <View style={styles.offscreen} pointerEvents="none">
        <ViewShot ref={shareRef} options={{ format: 'png', quality: 1 }}>
          <ShareCard title={task.title} body={`결론: ${task.conclusion}`} note={task.note} date={tasks.data.date} />
        </ViewShot>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.background },
  scroll: { padding: spacing(4), gap: spacing(5), paddingBottom: spacing(12) },
  report: { gap: spacing(2) },
  title: { ...typography.title, color: colors.ink },
  divider: { height: 2, backgroundColor: colors.border, opacity: 0.15, marginVertical: spacing(1) },
  label: { ...typography.caption, color: colors.derbyBlue, fontWeight: '700', marginTop: spacing(1) },
  body: { ...typography.body, color: colors.ink },
  followUpRow: { flexDirection: 'row', alignItems: 'flex-end', gap: spacing(2) },
  followUpEmoji: { fontSize: 24 },
  followUpBubble: {
    flex: 1,
    backgroundColor: colors.surface,
    borderWidth: 2,
    borderColor: colors.border,
    borderRadius: 12,
    borderBottomLeftRadius: 2,
    padding: spacing(3),
  },
  reactions: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing(3) },
  reactionButton: { flexBasis: '47%', flexGrow: 1 },
  bottomRow: { flexDirection: 'row', gap: spacing(3) },
  flex1: { flex: 1 },
  offscreen: { position: 'absolute', left: -9999, top: 0 },
});
