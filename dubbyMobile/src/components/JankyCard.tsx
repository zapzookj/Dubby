import type { PropsWithChildren } from 'react';
import { useMemo } from 'react';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

import { colors, jankyTilt, radii, spacing } from '@/theme/tokens';

interface JankyCardProps extends PropsWithChildren {
  /** 기울기 시드 — 항목 id. 결정적이어야 한다 (리렌더마다 흔들리면 진짜 버그처럼 보임) */
  seed: string | number;
  style?: StyleProp<ViewStyle>;
}

/** 흰 카드 + 굵은 외곽선 + 마스킹테이프 장식 + ±0.5° 결정적 기울기 */
export function JankyCard({ seed, style, children }: JankyCardProps) {
  const tilt = useMemo(() => jankyTilt(seed, 0.5), [seed]);
  return (
    <View style={[styles.card, { transform: [{ rotate: `${tilt}deg` }] }, style]}>
      <View style={styles.tape} />
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderWidth: 2,
    borderColor: colors.border,
    borderRadius: radii.card,
    padding: spacing(4),
    shadowColor: colors.border,
    shadowOpacity: 1,
    shadowRadius: 0,
    shadowOffset: { width: 3, height: 3 },
    elevation: 2,
  },
  tape: {
    position: 'absolute',
    top: -8,
    left: 24,
    width: 44,
    height: 16,
    backgroundColor: colors.tapeYellow,
    borderWidth: 1,
    borderColor: colors.border,
    transform: [{ rotate: '-4deg' }],
  },
});
