import { Redirect, router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import { DerbyAvatar } from '@/components/DerbyAvatar';
import { JankyButton } from '@/components/JankyButton';
import { Screen } from '@/components/Screen';
import { requestAndRegisterPush } from '@/notifications/setup';
import { useAppStateStore } from '@/stores/appStateStore';
import { onboardingDoneLabel, onboardingSteps } from '@/theme/copy';
import { colors, spacing, typography } from '@/theme/tokens';

/** 온보딩 — 타이핑 연출 4스텝 + 푸시 프리 프롬프트. 기획안 §12 */
export default function OnboardingScreen() {
  const completed = useAppStateStore((s) => s.onboardingCompleted);
  const completeOnboarding = useAppStateStore((s) => s.completeOnboarding);
  const recordPushPrompt = useAppStateStore((s) => s.recordPushPrompt);
  const [step, setStep] = useState(0);

  if (completed) {
    return <Redirect href="/" />;
  }

  const isLast = step === onboardingSteps.length - 1;

  // 프리 프롬프트(더비 톤) → 수락 시에만 OS 권한 요청. 거절해도 진행 (기획 §5.2)
  const finish = () => {
    recordPushPrompt();
    Alert.alert(
      '더비의 부탁',
      '더비가 가끔 사용자님을 찾아도 될까요?\n(하루 1번, 대체로 쓸데없는 보고입니다)',
      [
        {
          text: '나중에',
          style: 'cancel',
          onPress: () => {
            completeOnboarding();
            router.replace('/');
          },
        },
        {
          text: '허락하기',
          onPress: async () => {
            await requestAndRegisterPush().catch(() => {});
            completeOnboarding();
            router.replace('/');
          },
        },
      ],
    );
  };

  return (
    <Screen>
      <View style={styles.center}>
        <DerbyAvatar mood={isLast ? 'confident' : 'idle'} size={110} />
        <TypewriterText key={step} text={onboardingSteps[step]} />
        <View style={styles.dots}>
          {onboardingSteps.map((_, i) => (
            <View key={i} style={[styles.dot, i === step && styles.dotActive]} />
          ))}
        </View>
      </View>
      <View style={styles.footer}>
        {isLast ? (
          <JankyButton label={onboardingDoneLabel} seed="onboarding-done" onPress={finish} />
        ) : (
          <JankyButton label="다음" variant="secondary" seed={`next-${step}`} onPress={() => setStep(step + 1)} />
        )}
      </View>
    </Screen>
  );
}

/** 한 글자씩 타이핑, 탭하면 즉시 완성 */
function TypewriterText({ text }: { text: string }) {
  const [len, setLen] = useState(0);
  const done = len >= text.length;

  useEffect(() => {
    if (done) return;
    const timer = setInterval(() => setLen((l) => Math.min(l + 1, text.length)), 45);
    return () => clearInterval(timer);
  }, [done, text.length]);

  return (
    <Pressable onPress={() => setLen(text.length)} style={styles.typeBox} accessibilityRole="text">
      <Text style={styles.typeText}>
        {text.slice(0, len)}
        {!done && <Text style={styles.caret}>▍</Text>}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing(6),
  },
  typeBox: {
    minHeight: 110,
    paddingHorizontal: spacing(4),
    justifyContent: 'flex-start',
    alignSelf: 'stretch',
  },
  typeText: {
    ...typography.title,
    fontWeight: '500',
    color: colors.ink,
    textAlign: 'center',
  },
  caret: {
    color: colors.derbyBlue,
  },
  dots: {
    flexDirection: 'row',
    gap: spacing(2),
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: colors.inkSub,
    opacity: 0.3,
  },
  dotActive: {
    backgroundColor: colors.derbyBlue,
    opacity: 1,
  },
  footer: {
    paddingBottom: spacing(8),
    gap: spacing(3),
  },
});
