/**
 * 서버 API DTO 타입 — derby_system_spec_v1.md §5와 1:1.
 * 서버 스펙 문서에서 그대로 옮겨 적는다. 임의 필드 추가/변경 금지.
 */

// ── Auth ──
export interface DeviceAuthRequest {
  deviceId: string;
  platform: 'IOS' | 'ANDROID';
  timezone: string;
  locale?: string;
  appVersion?: string;
}

export interface DeviceAuthResponse {
  userId: string;
  isNewUser: boolean;
  accessToken: string;
  expiresIn: number;
}

// ── Settings ──
export interface SettingsResponse {
  nickname: string | null;
  timezone: string;
  prefs: Record<string, unknown>;
}

export interface SettingsPatchRequest {
  nickname?: string;
  timezone?: string;
  prefs?: Record<string, unknown>;
}

// ── Home ──
export type DerbyMood =
  | 'idle' | 'confident' | 'thinking' | 'panic'
  | 'collapsed' | 'happy' | 'sad' | 'sleeping';

export interface HomeResponse {
  derby: {
    mood: DerbyMood;
    statusLine: string;
    accuracy: string;
    currentWork: string;
  };
  todayTasks: { date: string; total: number; reactedCount: number };
  chatQuota: ChatQuota;
  diary: { totalEntries: number; pendingCandidates: number };
  billing: { tier: Tier; expiresAt: string | null };
}

export type Tier = 'FREE' | 'SUPPORTER' | 'SALARY';

// ── Tasks ──
export type Reaction = 'PRAISE' | 'SCOLD' | 'RETRY' | 'IGNORE';

export interface TaskButton {
  key: Reaction;
  label: string;
}

export interface TaskItem {
  assignmentId: number;
  templateCode: string;
  title: string;
  conclusion: string;
  note: string | null;
  buttons: TaskButton[];
  reaction: Reaction | null;
  retryCount: number;
  saved: boolean;
}

export interface TasksTodayResponse {
  date: string;
  tasks: TaskItem[];
}

export interface TaskReactionResponse {
  assignmentId: number;
  reaction: Reaction;
  followUpMessage: string;
}

// ── Chat ──
export interface ChatQuota {
  tier?: Tier;
  limit: number;
  used: number;
  remaining: number;
  resetsAt?: string;
  exhaustedMessage?: string;
}

export interface ChatMessage {
  id: number;
  role: 'USER' | 'DERBY';
  content: string;
  createdAt: string;
}

export interface SafetyNotice {
  category: 'SELF_HARM' | 'MEDICAL' | 'LEGAL' | 'FINANCE' | 'CRIME_VIOLENCE';
  title: string;
  body: string;
  resources: { label: string; action: string }[];
}

export interface ChatSendResponse {
  kind: 'DERBY' | 'SAFETY_NOTICE';
  userMessage?: ChatMessage;
  derbyMessage?: ChatMessage;
  diaryCandidate?: { candidateId: number; preview: string } | null;
  safetyNotice?: SafetyNotice;
  quota: ChatQuota;
}

// ── Diary ──
export interface DiaryEntry {
  entryId: number;
  fact: string;
  interpretation: string;
  conclusion: string;
  autoSaved: boolean;
  isShared: boolean;
  createdAt: string;
}

// ── 공통 ──
export interface CursorPage<T> {
  items: T[];
  nextCursor: number | null;
  hasNext: boolean;
}

export interface ApiErrorBody {
  code: string;
  message: string;
  derbyMessage: string;
  // 429 CHAT_LIMIT_EXCEEDED 확장 필드
  resetsAt?: string;
  paywallHint?: string;
}
