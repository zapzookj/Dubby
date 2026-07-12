import { StyleSheet, Text, View } from 'react-native';

import { DerbyApiError } from '@/api/client';
import { isSeriousZone } from '@/api/errorCodes';
import { API_BASE_URL } from '@/constants/config';
import { DerbyAvatar } from '@/components/DerbyAvatar';
import { JankyButton } from '@/components/JankyButton';
import { useUiStore } from '@/stores/uiStore';
import {
  consoleDerbyLabel,
  consoleDerbyReactions,
  genericErrorCopy,
  retryLabel,
} from '@/theme/copy';
import { colors, spacing, typography } from '@/theme/tokens';

interface DerbyErrorViewProps {
  error: unknown;
  onRetry?: () => void;
}

/**
 * 공통 에러 뷰 — derbyMessage + 명확한 [다시 시도].
 * 장난 금지 구역(AUTH_, BILLING_ 접두 코드)은 정자세 message 우선 표시.
 */
export function DerbyErrorView({ error, onRetry }: DerbyErrorViewProps) {
  const showToast = useUiStore((s) => s.showToast);

  let message = genericErrorCopy;
  let debugInfo = `대상 서버: ${API_BASE_URL}`;
  if (error instanceof DerbyApiError) {
    message = isSeriousZone(error.code) ? error.message : error.derbyMessage;
    debugInfo = `${error.code} (HTTP ${error.status || '-'}) · ${API_BASE_URL}`;
  } else if (error instanceof Error) {
    debugInfo = `${error.message} · ${API_BASE_URL}`;
  }

  return (
    <View style={styles.root}>
      <DerbyAvatar mood="panic" size={80} />
      <Text style={styles.message}>{message}</Text>
      {onRetry && <JankyButton label={retryLabel} onPress={onRetry} seed="retry" />}
      <JankyButton
        label={consoleDerbyLabel}
        variant="secondary"
        seed="console-derby"
        onPress={() =>
          showToast(consoleDerbyReactions[Math.floor(Math.random() * consoleDerbyReactions.length)])
        }
      />
      {/* 개발 중에만: 실제 원인/대상 서버 표기 — "예상한 척" 문구만으로 오진하는 것 방지 */}
      {__DEV__ && <Text style={styles.debug}>{debugInfo}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing(4),
    backgroundColor: colors.background,
    padding: spacing(6),
  },
  message: {
    ...typography.body,
    color: colors.ink,
    textAlign: 'center',
  },
  debug: {
    ...typography.caption,
    color: colors.inkSub,
    textAlign: 'center',
    marginTop: spacing(2),
  },
});
