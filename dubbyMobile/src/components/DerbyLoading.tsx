import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';

import { DerbyAvatar } from '@/components/DerbyAvatar';
import { colors, spacing, typography } from '@/theme/tokens';
import { loadingScripts } from '@/theme/copy';

interface DerbyLoadingProps {
  variant?: 'full' | 'mini';
  scriptKey?: keyof typeof loadingScripts;
  /** 설정 '로딩 중 헛소리 보기' OFF 시 평범한 스피너 */
  nonsense?: boolean;
}

/**
 * 로딩 개그 컴포넌트 — 페르소나 바이블 §11.
 * 가짜 퍼센트는 연출일 뿐이며 실제 응답 도착 시 즉시 언마운트된다(실 대기 시간을 늘리지 않는다).
 */
export function DerbyLoading({ variant = 'full', scriptKey = 'generic', nonsense = true }: DerbyLoadingProps) {
  const script = useMemo(() => {
    const sets = loadingScripts[scriptKey];
    return sets[Math.floor(Math.random() * sets.length)];
  }, [scriptKey]);

  const [step, setStep] = useState(0);

  useEffect(() => {
    if (!nonsense) return;
    const interval = setInterval(() => {
      setStep((s) => Math.min(s + 1, script.length - 1));
    }, 900);
    return () => clearInterval(interval);
  }, [nonsense, script.length]);

  // 가짜 진행률: 마지막 단계에서 99%에 머무름 (1.5초 상한은 인터벌 정지로 자연 충족)
  const fakePercent = Math.min(12 + step * 29, 99);

  if (variant === 'mini') {
    return (
      <View style={styles.miniRow}>
        <ActivityIndicator size="small" color={colors.derbyBlue} />
        {nonsense && <Text style={styles.miniText}>{script[step]}</Text>}
      </View>
    );
  }

  return (
    <View style={styles.full}>
      <DerbyAvatar mood="thinking" size={80} />
      {nonsense ? (
        <>
          <Text style={styles.line}>{script[step]}</Text>
          <Text style={styles.percent}>{fakePercent}%</Text>
        </>
      ) : (
        <>
          <ActivityIndicator size="large" color={colors.derbyBlue} style={{ marginTop: spacing(4) }} />
          <Text style={styles.line}>로딩 중...</Text>
        </>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  full: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing(3),
    backgroundColor: colors.background,
  },
  line: {
    ...typography.body,
    color: colors.inkSub,
    textAlign: 'center',
  },
  percent: {
    ...typography.caption,
    color: colors.derbyBlue,
    fontWeight: '700',
  },
  miniRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing(2),
  },
  miniText: {
    ...typography.caption,
    color: colors.inkSub,
  },
});
