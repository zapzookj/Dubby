import * as Haptics from 'expo-haptics';
import { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, type ViewStyle } from 'react-native';

import { colors, jankyTilt, radii, spacing } from '@/theme/tokens';

interface JankyButtonProps {
  label: string;
  onPress: () => void;
  /** 기울기 시드 — 리렌더에도 결정적. 미지정 시 label */
  seed?: string | number;
  variant?: 'primary' | 'secondary' | 'danger';
  disabled?: boolean;
  style?: ViewStyle;
}

/** 2px 잉크 외곽선 + 오프셋 하드 섀도 + 결정적 기울기. 기능은 정상, 표면만 하찮게 */
export function JankyButton({
  label,
  onPress,
  seed,
  variant = 'primary',
  disabled,
  style,
}: JankyButtonProps) {
  const tilt = useMemo(() => jankyTilt(seed ?? label), [seed, label]);
  const [pressed, setPressed] = useState(false);

  const bg =
    variant === 'primary' ? colors.derbyBlue : variant === 'danger' ? colors.accident : colors.surface;
  const fg = variant === 'secondary' ? colors.ink : '#FFFFFF';

  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      onPressIn={() => {
        setPressed(true);
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => {});
      }}
      onPressOut={() => setPressed(false)}
      onPress={onPress}
      style={[
        styles.base,
        {
          backgroundColor: bg,
          transform: [
            { rotate: `${tilt}deg` },
            { translateX: pressed ? 2 : 0 },
            { translateY: pressed ? 2 : 0 },
          ],
          shadowOffset: { width: pressed ? 1 : 3, height: pressed ? 1 : 3 },
          opacity: disabled ? 0.45 : 1,
        },
        style,
      ]}>
      <Text style={[styles.label, { color: fg }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: 48, // 터치 타깃 44pt+ 사수
    paddingHorizontal: spacing(5),
    paddingVertical: spacing(3),
    borderRadius: radii.button,
    borderWidth: 2,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: colors.border,
    shadowOpacity: 1,
    shadowRadius: 0,
    elevation: 3,
  },
  label: {
    fontSize: 15,
    fontWeight: '700',
  },
});
