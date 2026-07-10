import { Redirect, router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { DerbyAvatar } from '@/components/DerbyAvatar';
import { JankyButton } from '@/components/JankyButton';
import { Screen } from '@/components/Screen';
import { useAppStateStore } from '@/stores/appStateStore';
import { onboardingDoneLabel, onboardingSteps } from '@/theme/copy';
import { colors, spacing, typography } from '@/theme/tokens';

/** 온보딩 — 타이핑 연출 4스텝, 탭하면 즉시 완성. 기획안 §12 */
export default function OnboardingScreen() {
  const completed = useAppStateStore((s) => s.onboardingCompleted);
  const completeOnboarding = useAppStateStore((s) => s.completeOnboarding);
  const [step, setStep] = useState(0);

  if (completed) {
    return <Redirect href="/" />;
  }

  const isLast = step === onboardingSteps.length - 1;

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
          <JankyButton
            label={onboardingDoneLabel}
            seed="onboarding-done"
            onPress={() => {
              completeOnboarding();
              router.replace('/');
            }}
          />
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
