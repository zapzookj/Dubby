import { router } from 'expo-router';
import { StyleSheet, Text, View } from 'react-native';

import { DerbyAvatar } from '@/components/DerbyAvatar';
import { JankyButton } from '@/components/JankyButton';
import { Screen } from '@/components/Screen';
import { notFoundCopy } from '@/theme/copy';
import { colors, spacing, typography } from '@/theme/tokens';

export default function NotFoundScreen() {
  return (
    <Screen>
      <View style={styles.center}>
        <DerbyAvatar mood="panic" size={96} />
        <Text style={styles.text}>{notFoundCopy}</Text>
        <JankyButton label="홈으로" seed="not-found-home" onPress={() => router.replace('/')} />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing(5),
  },
  text: {
    ...typography.body,
    color: colors.ink,
    textAlign: 'center',
  },
});
