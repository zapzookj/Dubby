import type { PropsWithChildren } from 'react';
import { StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, spacing } from '@/theme/tokens';

interface ScreenProps extends PropsWithChildren {
  /** 기본 true — 헤더 없는 화면에서 상단 안전영역 패딩 */
  padTop?: boolean;
}

export function Screen({ children, padTop = true }: ScreenProps) {
  const insets = useSafeAreaInsets();
  return (
    <View
      style={[
        styles.root,
        {
          paddingTop: padTop ? insets.top + spacing(3) : spacing(3),
          paddingBottom: insets.bottom,
        },
      ]}>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: colors.background,
    paddingHorizontal: spacing(4),
  },
});
