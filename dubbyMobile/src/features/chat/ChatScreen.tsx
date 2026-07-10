import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import { useCallback, useMemo, useState } from 'react';
import {
  FlatList, KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, TextInput, View,
} from 'react-native';

import { DerbyApiError, generateUuid } from '@/api/client';
import { getChatMessages, getChatQuota, saveChatMessage, sendChatMessage } from '@/api/endpoints/chat';
import { approveDiaryCandidate, rejectDiaryCandidate } from '@/api/endpoints/diary';
import { ErrorCodes } from '@/api/errorCodes';
import { queryKeys } from '@/api/queryKeys';
import type { ChatSendResponse, SafetyNotice } from '@/api/types';
import { DerbyLoading } from '@/components/DerbyLoading';
import { JankyButton } from '@/components/JankyButton';
import { SafetyNoticeCard } from '@/components/SafetyNoticeCard';
import { useUiStore } from '@/stores/uiStore';
import { emptyStates } from '@/theme/copy';
import { colors, radii, spacing, typography } from '@/theme/tokens';

/** 채팅 리스트 아이템 (서버 이력 + 이번 세션 이벤트 통합) */
type ChatItem =
  | { kind: 'MSG'; key: string; id?: number; role: 'USER' | 'DERBY'; content: string }
  | { kind: 'SAFETY'; key: string; notice: SafetyNotice }
  | { kind: 'DIARY_CANDIDATE'; key: string; candidateId: number; preview: string }
  | { kind: 'ERROR'; key: string; derbyMessage: string; retry: { cmid: string; content: string } }
  | { kind: 'TYPING'; key: string };

export default function ChatScreen() {
  const queryClient = useQueryClient();
  const showToast = useUiStore((s) => s.showToast);

  const quota = useQuery({ queryKey: queryKeys.chatQuota, queryFn: getChatQuota, staleTime: 30_000 });
  const history = useQuery({
    queryKey: queryKeys.chatMessages,
    queryFn: () => getChatMessages(),
    staleTime: Infinity,
  });

  const [session, setSession] = useState<ChatItem[]>([]);
  const [input, setInput] = useState('');
  const pushSession = useCallback(
    (...items: ChatItem[]) => setSession((prev) => [...prev, ...items]),
    [],
  );
  const dropSession = useCallback(
    (key: string) => setSession((prev) => prev.filter((i) => i.key !== key)),
    [],
  );

  const send = useMutation({
    mutationFn: ({ cmid, content }: { cmid: string; content: string }) => sendChatMessage(cmid, content),
    onMutate: ({ cmid, content }) => {
      pushSession(
        { kind: 'MSG', key: `u-${cmid}`, role: 'USER', content },
        { kind: 'TYPING', key: `t-${cmid}` },
      );
    },
    onSuccess: (res: ChatSendResponse, { cmid }) => {
      dropSession(`t-${cmid}`);
      if (res.kind === 'SAFETY_NOTICE' && res.safetyNotice) {
        pushSession({ kind: 'SAFETY', key: `s-${cmid}`, notice: res.safetyNotice });
      } else if (res.derbyMessage) {
        pushSession({
          kind: 'MSG', key: `d-${cmid}`, id: res.derbyMessage.id,
          role: 'DERBY', content: res.derbyMessage.content,
        });
        if (res.diaryCandidate) {
          pushSession({
            kind: 'DIARY_CANDIDATE', key: `c-${cmid}`,
            candidateId: res.diaryCandidate.candidateId, preview: res.diaryCandidate.preview,
          });
        }
      }
      queryClient.setQueryData(queryKeys.chatQuota, (prev: object | undefined) =>
        prev ? { ...prev, ...res.quota } : res.quota);
      if (res.quota.remaining === 0) {
        router.push('/chat/exhausted');
      }
    },
    onError: (e, { cmid, content }) => {
      dropSession(`t-${cmid}`);
      if (e instanceof DerbyApiError && e.code === ErrorCodes.CHAT_LIMIT_EXCEEDED) {
        dropSession(`u-${cmid}`);
        setInput(content); // 입력 보존
        router.push('/chat/exhausted');
        return;
      }
      const message = e instanceof DerbyApiError ? e.derbyMessage : '더비가 대답을 흘렸습니다.';
      // [다시 시도]는 동일 clientMessageId 재전송 — 서버 멱등으로 이중 차감 없음
      pushSession({ kind: 'ERROR', key: `e-${cmid}-${Date.now()}`, derbyMessage: message, retry: { cmid, content } });
    },
  });

  const diaryAction = useMutation({
    mutationFn: async ({ candidateId, approve }: { candidateId: number; approve: boolean; key: string }) => {
      if (approve) {
        await approveDiaryCandidate(candidateId);
        return '더비가 일기장에 적어두었습니다.';
      }
      const res = await rejectDiaryCandidate(candidateId);
      return res.derbyMessage;
    },
    onSuccess: (message, { approve, key }) => {
      dropSession(key);
      showToast(message);
      if (approve) queryClient.invalidateQueries({ queryKey: queryKeys.diary });
    },
    onError: (e, { key }) => {
      if (e instanceof DerbyApiError) {
        showToast(e.derbyMessage);
        if (e.code === ErrorCodes.DIARY_CANDIDATE_NOT_FOUND) dropSession(key);
      }
    },
  });

  const saveMsg = useMutation({
    mutationFn: saveChatMessage,
    onSuccess: (res) => showToast(res.derbyMessage),
  });

  const handleSend = () => {
    const content = input.trim();
    if (!content || send.isPending) return;
    setInput('');
    send.mutate({ cmid: generateUuid(), content });
  };

  // 서버 이력(과거) + 세션 아이템(최신) → inverted 리스트용 역순
  const items = useMemo<ChatItem[]>(() => {
    const serverItems: ChatItem[] = (history.data?.items ?? [])
      .slice()
      .reverse()
      .map((m) => ({ kind: 'MSG', key: `h-${m.id}`, id: m.id, role: m.role, content: m.content }));
    return [...serverItems, ...session].reverse();
  }, [history.data, session]);

  if (history.isPending || quota.isPending) return <DerbyLoading scriptKey="chat" />;

  return (
    <KeyboardAvoidingView
      style={styles.root}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={90}>
      {quota.data && (
        <View style={styles.banner}>
          <Text style={styles.bannerText}>
            ⚠️ 더비는 가장 복잡하고 어려운 과제를 위한 최고성능의 AI 모델입니다. 정확도는 별개의 문제입니다.
          </Text>
          <Text style={styles.bannerCount}>남은 협의 횟수: {quota.data.remaining}회</Text>
        </View>
      )}

      <FlatList
        inverted
        data={items}
        keyExtractor={(item) => item.key}
        contentContainerStyle={styles.list}
        ListEmptyComponent={<Text style={styles.empty}>{emptyStates.chat}</Text>}
        renderItem={({ item }) => <ChatRow item={item}
          onRetry={(retry) => send.mutate(retry)}
          onDiary={(candidateId, approve, key) => diaryAction.mutate({ candidateId, approve, key })}
          onLongPressDerby={(id) => saveMsg.mutate(id)} />}
      />

      <View style={styles.inputRow}>
        <TextInput
          style={styles.input}
          value={input}
          onChangeText={setInput}
          placeholder="더비에게 일을 시켜보세요"
          placeholderTextColor={colors.inkSub}
          multiline
          maxLength={500}
        />
        <Pressable
          onPress={handleSend}
          disabled={send.isPending || !input.trim()}
          style={[styles.sendButton, (send.isPending || !input.trim()) && { opacity: 0.4 }]}>
          <Text style={styles.sendLabel}>전송</Text>
        </Pressable>
      </View>
    </KeyboardAvoidingView>
  );
}

function ChatRow({
  item, onRetry, onDiary, onLongPressDerby,
}: {
  item: ChatItem;
  onRetry: (retry: { cmid: string; content: string }) => void;
  onDiary: (candidateId: number, approve: boolean, key: string) => void;
  onLongPressDerby: (id: number) => void;
}) {
  switch (item.kind) {
    case 'MSG': {
      const isUser = item.role === 'USER';
      return (
        <Pressable
          disabled={isUser || !item.id}
          onLongPress={() => item.id && onLongPressDerby(item.id)}
          style={[styles.bubbleRow, isUser ? styles.rowUser : styles.rowDerby]}>
          {!isUser && <Text style={styles.derbyEmoji}>🤖</Text>}
          <View style={[styles.bubble, isUser ? styles.bubbleUser : styles.bubbleDerby]}>
            <Text style={[styles.bubbleText, isUser && { color: '#FFF' }]}>{item.content}</Text>
          </View>
        </Pressable>
      );
    }
    case 'TYPING':
      return (
        <View style={[styles.bubbleRow, styles.rowDerby]}>
          <Text style={styles.derbyEmoji}>🤖</Text>
          <View style={[styles.bubble, styles.bubbleDerby]}>
            <DerbyLoading variant="mini" scriptKey="chat" />
          </View>
        </View>
      );
    case 'SAFETY':
      return <SafetyNoticeCard notice={item.notice} />;
    case 'DIARY_CANDIDATE':
      return (
        <View style={styles.candidateCard}>
          <Text style={styles.candidateTitle}>📔 더비가 방금 일기장에 뭔가 적으려고 합니다</Text>
          <Text style={styles.candidatePreview} numberOfLines={2}>{item.preview}</Text>
          <View style={styles.candidateButtons}>
            <JankyButton label="적게 두기" seed={`ok-${item.key}`}
              onPress={() => onDiary(item.candidateId, true, item.key)} style={styles.flex1} />
            <JankyButton label="찢기" variant="secondary" seed={`no-${item.key}`}
              onPress={() => onDiary(item.candidateId, false, item.key)} style={styles.flex1} />
          </View>
        </View>
      );
    case 'ERROR':
      return (
        <View style={styles.errorCard}>
          <Text style={styles.bubbleText}>{item.derbyMessage}</Text>
          <View style={styles.candidateButtons}>
            <JankyButton label="다시 시도" seed={`retry-${item.key}`}
              onPress={() => onRetry(item.retry)} style={styles.flex1} />
          </View>
        </View>
      );
  }
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.background },
  banner: {
    backgroundColor: colors.tapeYellow,
    borderBottomWidth: 2,
    borderColor: colors.border,
    padding: spacing(3),
    gap: 2,
  },
  bannerText: { ...typography.caption, color: colors.ink },
  bannerCount: { ...typography.caption, fontWeight: '800', color: colors.ink },
  list: { padding: spacing(4), gap: spacing(3) },
  empty: { ...typography.body, color: colors.inkSub, textAlign: 'center', transform: [{ scaleY: -1 }] },
  bubbleRow: { flexDirection: 'row', alignItems: 'flex-end', gap: spacing(2), marginVertical: spacing(1) },
  rowUser: { justifyContent: 'flex-end' },
  rowDerby: { justifyContent: 'flex-start' },
  derbyEmoji: { fontSize: 22 },
  bubble: {
    maxWidth: '78%',
    borderWidth: 2,
    borderColor: colors.border,
    borderRadius: 14,
    paddingHorizontal: spacing(3),
    paddingVertical: spacing(2),
  },
  bubbleUser: { backgroundColor: colors.derbyBlue, borderBottomRightRadius: 2 },
  bubbleDerby: { backgroundColor: colors.surface, borderBottomLeftRadius: 2 },
  bubbleText: { ...typography.body, color: colors.ink },
  candidateCard: {
    backgroundColor: colors.surface,
    borderWidth: 2,
    borderColor: colors.derbyBlue,
    borderStyle: 'dashed',
    borderRadius: radii.card,
    padding: spacing(3),
    gap: spacing(2),
    marginVertical: spacing(1),
  },
  candidateTitle: { ...typography.caption, fontWeight: '800', color: colors.derbyBlue },
  candidatePreview: { ...typography.body, color: colors.inkSub },
  candidateButtons: { flexDirection: 'row', gap: spacing(2) },
  errorCard: {
    backgroundColor: colors.surface,
    borderWidth: 2,
    borderColor: colors.accident,
    borderRadius: radii.card,
    padding: spacing(3),
    gap: spacing(2),
  },
  flex1: { flex: 1 },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: spacing(2),
    padding: spacing(3),
    borderTopWidth: 2,
    borderColor: colors.border,
    backgroundColor: colors.surface,
  },
  input: {
    ...typography.body,
    flex: 1,
    maxHeight: 110,
    color: colors.ink,
    backgroundColor: colors.background,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.button,
    paddingHorizontal: spacing(3),
    paddingVertical: spacing(2),
  },
  sendButton: {
    backgroundColor: colors.derbyBlue,
    borderWidth: 2,
    borderColor: colors.border,
    borderRadius: radii.button,
    paddingHorizontal: spacing(4),
    minHeight: 44,
    justifyContent: 'center',
  },
  sendLabel: { color: '#FFF', fontWeight: '800' },
});
