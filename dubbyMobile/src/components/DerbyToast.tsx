import { useEffect } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import Animated, { FadeInDown, FadeOutDown } from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useUiStore } from '@/stores/uiStore';
import { colors, radii, spacing, typography } from '@/theme/tokens';

/** 전역 더비 토스트 — 루트 레이아웃에 1개 마운트. 연속 발생 시 교체(1큐) */
export function DerbyToastHost() {
  const toast = useUiStore((s) => s.toast);
  const clearToast = useUiStore((s) => s.clearToast);
  const insets = useSafeAreaInsets();

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(clearToast, 2600);
    return () => clearTimeout(timer);
  }, [toast, clearToast]);

  if (!toast) return null;

  return (
    <Animated.View
      key={toast.id}
      entering={FadeInDown.duration(200)}
      exiting={FadeOutDown.duration(200)}
      style={[styles.wrap, { bottom: insets.bottom + spacing(6) }]}
      pointerEvents="none">
      <View style={styles.bubble}>
        <Text style={styles.emoji}>🤖</Text>
        <Text style={styles.text}>{toast.message}</Text>
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    position: 'absolute',
    left: spacing(4),
    right: spacing(4),
    alignItems: 'center',
  },
  bubble: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing(2),
    backgroundColor: colors.surface,
    borderWidth: 2,
    borderColor: colors.border,
    borderRadius: radii.card,
    paddingHorizontal: spacing(4),
    paddingVertical: spacing(3),
    shadowColor: colors.border,
    shadowOpacity: 1,
    shadowRadius: 0,
    shadowOffset: { width: 2, height: 2 },
    elevation: 4,
    maxWidth: '100%',
  },
  emoji: { fontSize: 18 },
  text: {
    ...typography.body,
    color: colors.ink,
    flexShrink: 1,
  },
});
