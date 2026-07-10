import { StyleSheet, Text, View } from 'react-native';

import { colors, spacing } from '@/theme/tokens';

interface ShareCardProps {
  title: string;
  body: string;
  note?: string | null;
  date?: string;
}

/**
 * 공유용 캡처 카드 — 오프스크린 고정 크기(540x675, 4:5) 렌더 후 view-shot 캡처.
 * 하단 워터마크(바이럴 귀속) 고정.
 */
export function ShareCard({ title, body, note, date }: ShareCardProps) {
  return (
    <View style={styles.card}>
      <View style={styles.tape} />
      <Text style={styles.header}>더비의 업무 보고</Text>
      {date && <Text style={styles.date}>{date}</Text>}
      <View style={styles.content}>
        <Text style={styles.title}>✅ {title}</Text>
        <Text style={styles.body}>{body}</Text>
        {note && <Text style={styles.note}>비고: {note}</Text>}
      </View>
      <View style={styles.footer}>
        <Text style={styles.footerEmoji}>🤖</Text>
        <Text style={styles.footerText}>더비 — 도움이 필요한 AI 비서</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    width: 540,
    height: 675,
    backgroundColor: colors.background,
    borderWidth: 4,
    borderColor: colors.border,
    padding: spacing(8),
    justifyContent: 'space-between',
  },
  tape: {
    position: 'absolute',
    top: -2,
    alignSelf: 'center',
    width: 120,
    height: 30,
    backgroundColor: colors.tapeYellow,
    borderWidth: 2,
    borderColor: colors.border,
    transform: [{ rotate: '-2deg' }],
  },
  header: { fontSize: 22, fontWeight: '800', color: colors.derbyBlue, marginTop: spacing(6) },
  date: { fontSize: 14, color: colors.inkSub },
  content: { flex: 1, justifyContent: 'center', gap: spacing(4) },
  title: { fontSize: 28, lineHeight: 38, fontWeight: '700', color: colors.ink },
  body: { fontSize: 22, lineHeight: 32, color: colors.ink },
  note: { fontSize: 17, lineHeight: 24, color: colors.inkSub },
  footer: { flexDirection: 'row', alignItems: 'center', gap: spacing(2) },
  footerEmoji: { fontSize: 22 },
  footerText: { fontSize: 15, fontWeight: '600', color: colors.inkSub },
});
