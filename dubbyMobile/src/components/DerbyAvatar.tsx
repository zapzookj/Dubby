import { useEffect } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withSequence,
  withTiming,
} from 'react-native-reanimated';

import type { DerbyMood } from '@/api/types';
import { colors } from '@/theme/tokens';

/**
 * 더비 캐릭터 아바타.
 * TODO(외부 트랙): 표정 PNG 8종(assets/derby/{mood}.png) 수급 시 이모지 → 이미지 교체.
 * 컴포넌트 계약(mood props)은 유지되므로 교체 지점은 이 파일뿐이다.
 */
const MOOD_EMOJI: Record<DerbyMood, string> = {
  idle: '🤖',
  confident: '😤',
  thinking: '🤔',
  panic: '😰',
  collapsed: '😵',
  happy: '😊',
  sad: '😢',
  sleeping: '😴',
};

interface DerbyAvatarProps {
  mood?: DerbyMood;
  size?: number;
  /** idle 부유 애니메이션 (기본 on) */
  animated?: boolean;
}

export function DerbyAvatar({ mood = 'idle', size = 96, animated = true }: DerbyAvatarProps) {
  const floatY = useSharedValue(0);

  useEffect(() => {
    if (!animated) return;
    // 2~4초 주기 미세 상하 부유 + 가끔 기울어짐 — 이미지 1장으로 하찮게 살아있음
    floatY.value = withRepeat(
      withSequence(
        withTiming(-4, { duration: 1400 }),
        withTiming(2, { duration: 1600 }),
        withTiming(0, { duration: 1000 }),
      ),
      -1,
      true,
    );
  }, [animated, floatY]);

  const style = useAnimatedStyle(() => ({
    transform: [{ translateY: floatY.value }],
  }));

  return (
    <Animated.View style={[styles.circle, { width: size, height: size, borderRadius: size / 2 }, style]}>
      <Text style={{ fontSize: size * 0.5 }}>{MOOD_EMOJI[mood]}</Text>
      <View style={styles.badge}>
        <Text style={styles.badgeText}>더비</Text>
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  circle: {
    backgroundColor: colors.surface,
    borderWidth: 2,
    borderColor: colors.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badge: {
    position: 'absolute',
    bottom: -6,
    backgroundColor: colors.tapeYellow,
    borderWidth: 1,
    borderColor: colors.border,
    paddingHorizontal: 6,
    borderRadius: 4,
  },
  badgeText: {
    fontSize: 10,
    fontWeight: '700',
    color: colors.ink,
  },
});
