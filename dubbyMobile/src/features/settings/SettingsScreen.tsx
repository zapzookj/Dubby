import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as Application from 'expo-application';
import { useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Switch, Text, TextInput, View } from 'react-native';
import Animated, { useAnimatedStyle, useSharedValue, withSpring } from 'react-native-reanimated';

import { DerbyApiError } from '@/api/client';
import { getPushSettings, putPushSettings } from '@/api/endpoints/push';
import { deleteAccount, getSettings, patchSettings } from '@/api/endpoints/settings';
import { queryKeys } from '@/api/queryKeys';
import { DerbyErrorView } from '@/components/DerbyErrorView';
import { DerbyLoading } from '@/components/DerbyLoading';
import { JankyButton } from '@/components/JankyButton';
import { useAppStateStore } from '@/stores/appStateStore';
import { useAuthStore } from '@/stores/authStore';
import { useUiStore } from '@/stores/uiStore';
import { colors, radii, spacing, typography } from '@/theme/tokens';

export default function SettingsScreen() {
  const settings = useQuery({ queryKey: queryKeys.settings, queryFn: getSettings, staleTime: 5 * 60_000 });
  const queryClient = useQueryClient();
  const showToast = useUiStore((s) => s.showToast);

  const patch = useMutation({
    mutationFn: patchSettings,
    onSuccess: (data) => queryClient.setQueryData(queryKeys.settings, data),
    onError: (e) => e instanceof DerbyApiError && showToast(e.derbyMessage),
  });

  if (settings.isPending) return <DerbyLoading />;
  if (settings.isError) return <DerbyErrorView error={settings.error} onRetry={() => settings.refetch()} />;

  const prefs = settings.data.prefs as Record<string, boolean>;
  const setPref = (key: string, value: boolean) => patch.mutate({ prefs: { [key]: value } });

  return (
    <ScrollView style={styles.root} contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
      <Section title="더비 담당자 정보">
        <NicknameRow
          current={settings.data.nickname}
          onSubmit={(nickname) => patch.mutate({ nickname })}
        />
      </Section>

      <Section title="더비 관리">
        <SettingRow
          label="로딩 중 헛소리 보기"
          sub="더비가 생각하는 척하는 과정을 표시합니다."
          value={prefs.loadingNonsense !== false}
          onChange={(v) => setPref('loadingNonsense', v)}
        />
        <RunawaySettingRow
          label="더비가 똑똑해졌다고 믿기"
          sub="실제 성능과 무관한 신앙 기반 설정입니다."
          value={prefs.believeSmarter !== false}
          onChange={(v) => setPref('believeSmarter', v)}
        />
        <SettingRow
          label="위험한 자신감 허용"
          sub="더비가 근거 없이 당당해질 수 있습니다."
          value={prefs.dangerousConfidence !== false}
          onChange={(v) => {
            setPref('dangerousConfidence', v);
            if (!v) showToast('더비가 근거 있는 말만 하려고 시도합니다. 실패할 수 있습니다.');
          }}
        />
        <PushSettingsRows />
        <FakeSettingRow label="더비의 분리 불안 관리" status="점검 중" />
      </Section>

      <Section title="다크모드">
        <Text style={styles.plainSub}>더비가 어둠을 무서워해서 준비 중입니다.</Text>
      </Section>

      {/* 장난 금지 구역 — 명확성 우선 */}
      <Section title="계정" serious>
        <DeleteAccountRow />
      </Section>

      <Text style={styles.version}>
        더비 v{Application.nativeApplicationVersion ?? '0.1.0'} (더비 기준 최신)
      </Text>
    </ScrollView>
  );
}

function Section({ title, serious, children }: { title: string; serious?: boolean; children: React.ReactNode }) {
  return (
    <View style={styles.section}>
      <Text style={[styles.sectionTitle, serious && styles.seriousTitle]}>{title}</Text>
      <View style={styles.sectionBody}>{children}</View>
    </View>
  );
}

function SettingRow({
  label, sub, value, onChange, disabled,
}: { label: string; sub?: string; value: boolean; onChange: (v: boolean) => void; disabled?: boolean }) {
  return (
    <View style={styles.row}>
      <View style={styles.rowText}>
        <Text style={styles.rowLabel}>{label}</Text>
        {sub && <Text style={styles.rowSub}>{sub}</Text>}
      </View>
      <Switch
        value={value}
        disabled={disabled}
        onValueChange={onChange}
        trackColor={{ true: colors.derbyBlue, false: '#C9C4B4' }}
        thumbColor={colors.surface}
      />
    </View>
  );
}

/** 이스터에그: OFF 시도 시 스위치가 1회 도망 → 두 번째 시도는 무조건 정상 동작 (기획 §14.3) */
function RunawaySettingRow(props: { label: string; sub?: string; value: boolean; onChange: (v: boolean) => void }) {
  const fireEasterEgg = useUiStore((s) => s.fireEasterEgg);
  const showToast = useUiStore((s) => s.showToast);
  const offsetX = useSharedValue(0);
  const animStyle = useAnimatedStyle(() => ({ transform: [{ translateX: offsetX.value }] }));

  const handleChange = (v: boolean) => {
    if (!v && fireEasterEgg('runaway-believe-smarter')) {
      // 1회 장난: 스위치가 도망갔다 돌아온다. 설정은 바꾸지 않음
      offsetX.value = withSpring(-56, { damping: 6 }, () => {
        offsetX.value = withSpring(0);
      });
      showToast('이 믿음은 더비의 핵심 인프라입니다. 정말 끄시겠습니까?');
      return;
    }
    props.onChange(v);
  };

  return (
    <View style={styles.row}>
      <View style={styles.rowText}>
        <Text style={styles.rowLabel}>{props.label}</Text>
        {props.sub && <Text style={styles.rowSub}>{props.sub}</Text>}
      </View>
      <Animated.View style={animStyle}>
        <Switch
          value={props.value}
          onValueChange={handleChange}
          trackColor={{ true: colors.derbyBlue, false: '#C9C4B4' }}
          thumbColor={colors.surface}
        />
      </Animated.View>
    </View>
  );
}

/** 알림 설정 — OFF는 이스터에그 다이얼로그 1회 후 확실히 반영 (서버 발송 중단) */
function PushSettingsRows() {
  const queryClient = useQueryClient();
  const showToast = useUiStore((s) => s.showToast);
  const pushSettings = useQuery({
    queryKey: ['push', 'settings'],
    queryFn: getPushSettings,
    staleTime: 5 * 60_000,
  });

  const update = useMutation({
    mutationFn: putPushSettings,
    onSuccess: (data) => queryClient.setQueryData(['push', 'settings'], data),
    onError: (e) => e instanceof DerbyApiError && showToast(e.derbyMessage),
  });

  if (!pushSettings.data) {
    return (
      <View style={styles.row}>
        <View style={styles.rowText}>
          <Text style={styles.rowLabel}>알림 받기</Text>
          <Text style={styles.rowSub}>더비가 알림 설정을 찾는 중입니다...</Text>
        </View>
      </View>
    );
  }

  const { enabled, maxDailyCount } = pushSettings.data;

  const handleToggle = (value: boolean) => {
    if (!value) {
      // 분리 불안 이스터에그 — 1회 다이얼로그 후 선택은 확실히 존중
      Alert.alert('알림을 끄시겠습니까?', '더비의 분리 불안이 심해질 수 있습니다.', [
        { text: '더비를 안심시키고 유지', style: 'cancel' },
        {
          text: '그래도 끄기',
          style: 'destructive',
          onPress: () => update.mutate({ enabled: false, maxDailyCount }),
        },
      ]);
      return;
    }
    update.mutate({ enabled: true, maxDailyCount });
  };

  return (
    <>
      <SettingRow
        label="알림 받기"
        sub="더비가 가끔 사용자님을 찾습니다. 이유는 그때그때 지어냅니다."
        value={enabled}
        onChange={handleToggle}
      />
      {enabled && (
        <View style={styles.row}>
          <View style={styles.rowText}>
            <Text style={styles.rowLabel}>더비를 견디는 횟수</Text>
            <Text style={styles.rowSub}>하루 최대 알림 수</Text>
          </View>
          <View style={styles.countRow}>
            {[1, 2, 3].map((n) => (
              <Pressable
                key={n}
                onPress={() => update.mutate({ enabled: true, maxDailyCount: n })}
                style={[styles.countChip, maxDailyCount === n && styles.countChipActive]}>
                <Text style={[styles.countChipText, maxDailyCount === n && { color: '#FFF' }]}>{n}</Text>
              </Pressable>
            ))}
          </View>
        </View>
      )}
    </>
  );
}

/** 기능 없는 개그 항목 — 토글 대신 상태 텍스트만 */
function FakeSettingRow({ label, status }: { label: string; status: string }) {
  const showToast = useUiStore((s) => s.showToast);
  return (
    <View style={styles.row}>
      <View style={styles.rowText}>
        <Text style={styles.rowLabel} onPress={() => showToast('점검이 언제 끝나는지는 더비도 모릅니다.')}>
          {label}
        </Text>
      </View>
      <Text style={styles.fakeStatus}>{status}</Text>
    </View>
  );
}

function NicknameRow({ current, onSubmit }: { current: string | null; onSubmit: (v: string) => void }) {
  const [value, setValue] = useState(current ?? '');
  return (
    <View style={styles.row}>
      <View style={styles.rowText}>
        <Text style={styles.rowLabel}>부르는 이름</Text>
        <Text style={styles.rowSub}>비워두면 “사용자님”이라고 부릅니다.</Text>
      </View>
      <TextInput
        style={styles.nicknameInput}
        value={value}
        onChangeText={setValue}
        onEndEditing={() => value !== (current ?? '') && onSubmit(value)}
        placeholder="사용자님"
        placeholderTextColor={colors.inkSub}
        maxLength={12}
      />
    </View>
  );
}

/** 계정 삭제 — 장난 금지 구역: 이중 확인 + 명확한 문구 */
function DeleteAccountRow() {
  const bootstrap = useAuthStore((s) => s.bootstrap);
  const queryClient = useQueryClient();

  const del = useMutation({
    mutationFn: deleteAccount,
    onSuccess: async () => {
      queryClient.clear();
      useAppStateStore.setState({ onboardingCompleted: false });
      await bootstrap(); // 동일 deviceId → 새 계정 발급, 온보딩부터 재시작
    },
  });

  const confirm = () =>
    Alert.alert(
      '계정 삭제',
      '계정과 모든 데이터(업무 기록, 대화, 일기)가 즉시 삭제됩니다. 이 작업은 되돌릴 수 없습니다.',
      [
        { text: '취소', style: 'cancel' },
        {
          text: '삭제',
          style: 'destructive',
          onPress: () =>
            Alert.alert('정말 삭제하시겠습니까?', '삭제 후에는 복구할 수 없습니다.', [
              { text: '취소', style: 'cancel' },
              { text: '영구 삭제', style: 'destructive', onPress: () => del.mutate() },
            ]),
        },
      ],
    );

  return (
    <JankyButton
      label={del.isPending ? '삭제 중...' : '계정 및 데이터 삭제'}
      variant="danger"
      seed="delete-account"
      disabled={del.isPending}
      onPress={confirm}
    />
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.background },
  scroll: { padding: spacing(4), gap: spacing(6), paddingBottom: spacing(12) },
  section: { gap: spacing(2) },
  sectionTitle: { ...typography.caption, color: colors.derbyBlue, fontWeight: '800', letterSpacing: 1 },
  seriousTitle: { color: colors.accident },
  sectionBody: {
    backgroundColor: colors.surface,
    borderWidth: 2,
    borderColor: colors.border,
    borderRadius: radii.card,
    padding: spacing(3),
    gap: spacing(3),
  },
  row: { flexDirection: 'row', alignItems: 'center', gap: spacing(3), minHeight: 44 },
  rowText: { flex: 1, gap: 2 },
  rowLabel: { ...typography.body, fontWeight: '600', color: colors.ink },
  rowSub: { ...typography.caption, color: colors.inkSub },
  fakeStatus: { ...typography.caption, color: colors.inkSub, fontStyle: 'italic' },
  plainSub: { ...typography.body, color: colors.inkSub },
  countRow: { flexDirection: 'row', gap: spacing(2) },
  countChip: {
    width: 36,
    height: 36,
    borderRadius: 18,
    borderWidth: 2,
    borderColor: colors.border,
    backgroundColor: colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  countChipActive: { backgroundColor: colors.derbyBlue },
  countChipText: { ...typography.body, fontWeight: '700', color: colors.ink },
  nicknameInput: {
    ...typography.body,
    color: colors.ink,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radii.button,
    paddingHorizontal: spacing(3),
    paddingVertical: spacing(2),
    minWidth: 110,
    textAlign: 'center',
    backgroundColor: colors.background,
  },
  version: { ...typography.caption, color: colors.inkSub, textAlign: 'center', marginTop: spacing(4) },
});
