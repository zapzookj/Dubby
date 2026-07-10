import { Linking, StyleSheet, Text, View } from 'react-native';

import type { SafetyNotice } from '@/api/types';

/**
 * 고위험 입력 시스템 카드 — 페르소나 바이블 §6.4.
 * 더비 말풍선이 아닌 중립 카드: 개그 팔레트/더비 아바타/기울기/더비 폰트 미적용.
 * 저장/공유 버튼 없음. tel: 리소스는 딥링크 버튼.
 */
export function SafetyNoticeCard({ notice }: { notice: SafetyNotice }) {
  return (
    <View style={styles.card}>
      <Text style={styles.title}>{notice.title}</Text>
      <Text style={styles.body}>{notice.body}</Text>
      {notice.resources.map((r) => (
        <Text
          key={r.action}
          style={styles.link}
          accessibilityRole="link"
          onPress={() => Linking.openURL(r.action).catch(() => {})}>
          {r.label}
        </Text>
      ))}
    </View>
  );
}

// 의도적으로 앱 테마(하찮음)와 분리된 중립 스타일
const styles = StyleSheet.create({
  card: {
    backgroundColor: '#F2F3F5',
    borderWidth: 1,
    borderColor: '#C7CBD1',
    borderRadius: 8,
    padding: 16,
    gap: 8,
    marginVertical: 6,
  },
  title: { fontSize: 14, fontWeight: '700', color: '#3A3F45' },
  body: { fontSize: 15, lineHeight: 22, color: '#3A3F45' },
  link: {
    fontSize: 15,
    fontWeight: '700',
    color: '#1A6DB3',
    paddingVertical: 8,
    textDecorationLine: 'underline',
  },
});
