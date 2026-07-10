# dubbyMobile 아키텍처 v1

> **문서 목적:** React Native + Expo + TypeScript 클라이언트의 구현 기준서.
> API 계약·에러 코드는 [derby_system_spec_v1.md](derby_system_spec_v1.md)가 SSOT — 본 문서의 모든 경로/필드는 그 문서와 1:1이다.
> 원칙: 클라이언트는 "더비 연기"를 렌더링하는 무대다. LLM 호출·템플릿 선정·쿨다운·기능 제한 판정은 전부 백엔드 책임이며, 클라이언트는 절대 OpenRouter를 직접 호출하지 않는다.
>
> **버전:** v1.0 (2026-07-10 승인)

---

## 1. 프로젝트 초기화

```bash
npx create-expo-app@latest dubbyMobile --template default
# default 템플릿: TypeScript + Expo Router + New Architecture 기본 포함
# 생성 직후 npm run reset-project로 예제 제거, 빈 app/에서 시작
```

- Expo SDK: 생성 시점 최신 안정 버전 고정. 업데이트는 `npx expo install --fix`로만.
- **Development Build 기준 개발** (`npx expo run:android` / EAS Build) — `react-native-purchases`는 Expo Go에서 동작하지 않음. 결제 미연동 UI 작업은 Expo Go 병행 가능.

추가 의존성:

```bash
npx expo install @tanstack/react-query zustand
npx expo install expo-secure-store @react-native-async-storage/async-storage
npx expo install expo-notifications expo-device expo-constants
npx expo install react-native-purchases
npx expo install expo-localization expo-haptics expo-application expo-font
npx expo install react-native-reanimated react-native-view-shot expo-sharing
```

Lottie는 MVP 미도입 (§8.1). axios 미도입 — fetch 래퍼로 충분.

`app.config.ts` 핵심: `scheme: "dubby"`(딥링크), `newArchEnabled: true`, plugins(expo-router, expo-secure-store, expo-notifications), 환경 분기 `process.env.APP_VARIANT`. 클라이언트에 존재하는 키는 `EXPO_PUBLIC_API_BASE_URL`과 RevenueCat public SDK key뿐 — **그 외 어떤 시크릿도 클라이언트에 두지 않는다.**

---

## 2. 디렉토리 구조

```text
dubbyMobile/
├── app/                            # Expo Router 라우트 (껍데기만 — 3줄 이내 re-export)
│   ├── _layout.tsx                 # Provider 조립 + 인증/온보딩 가드 + 푸시 리스너
│   ├── onboarding.tsx
│   ├── (main)/
│   │   ├── _layout.tsx             # 메인 Stack (더비 테마 헤더)
│   │   ├── index.tsx               # 홈
│   │   ├── tasks/index.tsx         # 오늘의 업무 리스트
│   │   ├── tasks/[assignmentId].tsx# 업무 상세/반응
│   │   ├── chat/index.tsx          # 채팅
│   │   ├── chat/exhausted.tsx      # 과로(채팅 제한) 화면
│   │   ├── diary/index.tsx         # 일기장
│   │   ├── diary/[entryId].tsx     # 일기 상세
│   │   └── settings.tsx
│   ├── salary.tsx                  # 월급(Paywall) — modal presentation
│   └── +not-found.tsx              # "이 화면은 더비가 잃어버렸습니다"
│
├── src/
│   ├── api/
│   │   ├── client.ts               # fetch 래퍼: baseURL, JWT 주입, 401 재인증, 에러 정규화
│   │   ├── types.ts                # DTO 타입 — 시스템 스펙 §5에서 그대로 옮겨 적기 (1:1 원칙)
│   │   ├── errorCodes.ts           # 에러 코드 상수 — 스펙 §6과 문자열 동일
│   │   ├── queryKeys.ts            # Query 키 팩토리 (전역 단일 소스)
│   │   └── endpoints/              # 도메인별 API 순수 함수
│   │       ├── auth.ts             # POST /auth/device
│   │       ├── home.ts             # GET /home
│   │       ├── tasks.ts            # GET /tasks/today, /tasks/saved, POST .../reaction|save|share
│   │       ├── chat.ts             # GET /chat/quota, GET/POST /chat/messages, POST .../save
│   │       ├── diary.ts            # candidates approve/reject, entries CRUD/share/rewrite
│   │       ├── push.ts             # POST /push/tokens, /push/tokens/delete, GET/PUT /push/settings, POST /push/logs/{id}/open
│   │       ├── settings.ts         # GET/PATCH /settings, DELETE /users/me
│   │       └── billing.ts          # GET /billing/me, POST /billing/sync
│   ├── features/                   # 화면 단위 로직+UI
│   │   ├── onboarding/             # OnboardingScreen, TypewriterText, steps.ts(문구 하드코딩)
│   │   ├── home/                   # HomeScreen, DerbyStatusCard, MenuGrid
│   │   ├── tasks/                  # TaskListScreen, TaskDetailScreen, ReactionButtons, FollowUpBubble
│   │   ├── chat/                   # ChatScreen, MessageBubble, SafetyNoticeCard, QuotaBanner, ExhaustedScreen, DiaryCandidateSheet
│   │   ├── diary/                  # DiaryListScreen, DiaryEntryCard, DeleteConfirm
│   │   ├── settings/               # SettingsScreen, EasterEggSwitch, RunawayButton, FakeSettingRow
│   │   └── salary/                 # SalaryScreen(Paywall), ProductCard, CoffeeCard, RestoreButton
│   ├── components/                 # 화면 무관 공용
│   │   ├── DerbyLoading.tsx        # 로딩 개그 (full/mini)
│   │   ├── DerbyErrorView.tsx      # derbyMessage + 명확한 [다시 시도]
│   │   ├── DerbyAvatar.tsx         # mood별 표정 PNG
│   │   ├── DerbyToast.tsx          # 더비 코멘트 토스트 (전역 1큐)
│   │   ├── SafetyNoticeCard.tsx    # 고위험 입력 시스템 카드 (§3.5)
│   │   ├── ShareCard.tsx           # 공유용 오프스크린 캡처 카드 (1080x1350)
│   │   ├── JankyButton.tsx / JankyCard.tsx / Screen.tsx
│   ├── stores/                     # Zustand
│   │   ├── authStore.ts            # status: 'loading'|'guest'|'error', userId (토큰 원본은 SecureStore)
│   │   ├── appStateStore.ts        # persist: 온보딩 완료, 마지막 푸시 프롬프트 시각
│   │   └── uiStore.ts              # 휘발성: 이스터에그 발동 이력, pendingDeepLink, 채팅 draft
│   ├── notifications/
│   │   ├── setup.ts                # 권한 요청, 토큰 발급/등록, Android 채널
│   │   └── router.ts               # 푸시 payload → 라우트 매핑
│   ├── purchases/revenuecat.ts     # configure, usePremium(), 서버 sync
│   ├── theme/
│   │   ├── tokens.ts               # 컬러/타이포/간격 토큰
│   │   └── copy.ts                 # ★ 클라 하드코딩 더비 문구 전부 이 한 파일 (로딩/에러/이스터에그/온보딩 폴백)
│   ├── constants/config.ts
│   └── assets/derby/               # 표정 PNG 세트
├── app.config.ts
└── tsconfig.json                   # paths: "@/*" → "src/*"
```

**규칙:**
- 서버 데이터 접근은 `endpoints/` 함수 → feature 내 `useXxxQuery` 훅 경유. 컴포넌트에서 fetch 직접 호출 금지.
- 서버가 내려주는 더비 문구(derbyMessage, 템플릿 content)는 그대로 렌더링. 클라 하드코딩 문구는 `theme/copy.ts` 한 파일에만(페르소나 검수 지점 단일화) — 로딩/에러/설정 이스터에그/온보딩은 오프라인에서도 떠야 하므로 클라 소유.

---

## 3. 화면별 설계

루트 가드: 앱 시작 → SecureStore JWT 로드(없으면 `POST /auth/device`) → `onboardingCompleted` false면 `/onboarding`, true면 `/(main)`.

### 3.1 온보딩 `/onboarding`
- `TypewriterText`(한 글자씩, 탭하면 즉시 완성) × 기획안 §12.2 문구 4스텝. 번들 하드코딩(`features/onboarding/steps.ts`).
- 마지막 스텝: 푸시 프리 프롬프트("더비가 가끔 사용자님을 찾아도 될까요?") → 수락 시 OS 권한 요청 + 토큰 등록. 거절해도 진행.
- 완료: `appStateStore.completeOnboarding()` → `router.replace('/')`.

### 3.2 홈 `/(main)/index`
- `GET /home` 1회 호출로 전체 구성 (스펙 §5.2 응답). `DerbyAvatar(mood)`, `DerbyStatusCard`(statusLine/accuracy/currentWork), `MenuGrid`, 미반응 업무 배지.
- 캐시: `staleTime 60s`, AppState active 복귀 시 refetch.

### 3.3 오늘의 업무 `/(main)/tasks` + `[assignmentId]`
- `GET /tasks/today`. queryKey `['tasks','today', localDate]` — **로컬 날짜를 키에 포함**해 자정 넘김 시 자동 새 캐시.
- 상세: 반응 버튼 4종(PRAISE/SCOLD/RETRY/IGNORE) → `POST /tasks/{id}/reaction` → `followUpMessage`를 말풍선 표시. 반응은 낙관적 업데이트 금지(후속 문구가 서버에서 와야 함) — 버튼 인라인 `DerbyLoading(mini)`.
- **덮어쓰기 허용** — 마지막 반응 하이라이트 표시(버튼 disabled 아님). RETRY는 서버 `TASK_RETRY_EXHAUSTED`(409) 수신 시 고정 문구 표시.
- 저장 `POST .../save`, 공유 `POST .../share` + `ShareCard` 캡처 → `expo-sharing` 시트(§8.5).
- 저장한 업무 리스트: 일기장 탭 하위 섹션, `GET /tasks/saved`.

### 3.4 채팅 `/(main)/chat`
- 진입: `GET /chat/quota` → `QuotaBanner`(Warning 문구 + 남은 횟수). 이력: `GET /chat/messages?cursor=` (`useInfiniteQuery`, inverted FlatList).
- 전송: `POST /chat/messages { clientMessageId: uuid(), content }`. **MVP 논스트리밍** — 대기 시간은 `TypingIndicator`("아는 척 하는 중...")로 마스킹. 응답 성공 시 setQueryData로 두 메시지 append + quota 갱신.
- **응답 분기 (`kind`):**
  - `DERBY` → 더비 말풍선 + `diaryCandidate` 있으면 `DiaryCandidateSheet`("더비가 방금 일기장에 뭔가 적으려고 합니다" [적게 두기]→approve / [찢기]→reject).
  - `SAFETY_NOTICE` → **`SafetyNoticeCard`** (§3.5).
- 에러 분기(코드 문자열은 `errorCodes.ts` 상수 — 서버와 동일): `CHAT_LIMIT_EXCEEDED` → `/chat/exhausted` 라우팅. `LLM_UPSTREAM_ERROR`/`LLM_TIMEOUT` → 말풍선 자리 에러 카드 + [다시 시도](**같은 clientMessageId로 재전송** — 멱등, 입력 보존) + [더비 위로하기](로컬 개그).
- 메시지 길게 누르기 → "저장" (`POST /chat/messages/{id}/save`).

### 3.5 SafetyNoticeCard (페르소나 바이블 §6.4 — 프론트가 렌더링 주체)
- 더비 말풍선이 아닌 **중립 회백색 전각 시스템 카드**. 더비 아바타·개그 애니메이션·더비 폰트 **미적용**. 저장/공유 버튼 없음.
- `resources[].action`이 `tel:`이면 `Linking.openURL` 버튼 렌더 (SELF_HARM).
- 카드는 대화 스크롤에 남고, 더비는 다음 메시지부터 평소처럼 복귀.

### 3.6 과로 화면 `/(main)/chat/exhausted`
- `DerbyAvatar(mood="collapsed")` + `GET /chat/quota`의 `exhaustedMessage` + `resetsAt` 기반 "내일 HH시에 회복" 표기.
- 버튼: [더비에게 커피 사주기] → `/salary?focus=coffee` / [내일 다시 괴롭히기] → 홈. **결제 압박 금지 — 버튼 1개, 감정 조작 문구 없음.**

### 3.7 일기장 `/(main)/diary` + `[entryId]`
- `GET /diary/entries?cursor=` (useInfiniteQuery). 빈 화면: "아직 배운 게 없습니다. 정확히는, 배웠는데 까먹었습니다."
- 슬롯 표기는 서버 응답 필드 기반(하드코딩 금지). 상세는 리스트 캐시 `initialData`로 즉시 렌더.
- 삭제: 확인 다이얼로그(페르소나 §9.6 문구) → `DELETE /diary/entries/{id}` — 낙관적 업데이트 허용(실패 시 롤백). **삭제는 반드시 확실히 동작.**
- SALARY 전용: [다시 쓰게 하기] → `POST .../rewrite`.

### 3.8 설정 `/(main)/settings`
- 실동작 설정: 알림 ON/OFF·횟수(`PUT /push/settings`), 닉네임/타임존(`PATCH /settings`), 데이터·계정 삭제(`DELETE /users/me`, 이중 확인 — 장난 금지 구역).
- 이스터에그(§8.4): `RunawayButton`, 분리 불안 다이얼로그, `FakeSettingRow`. prefs는 서버 `PATCH /settings { prefs }` 저장(재설치에도 유지되는 개그).
- 법적 고지 링크(약관/개인정보) — 정자세 표기.

### 3.9 월급(Paywall) `/salary` (modal)
- RevenueCat `getOfferings()` → `ProductCard`(월급 구독)·`CoffeeCard` 렌더. **가격/갱신조건/해지 안내는 장난 카피와 시각적으로 분리**(시스템 폰트·정렬 = "장난 아님" 신호).
- 구매: `purchasePackage()` → 성공 시 `POST /billing/sync` + `['home']`,`['chat','quota']`,`['billing']` invalidate → 효과(채팅 횟수)가 바로 보임.
- `RestoreButton`(구매 복원) 하단 필수 — iOS 심사 요건.

### 3.10 공통 로딩/에러
- `DerbyLoading`: §8.2. `DerbyErrorView`: `{ code, message, derbyMessage }` 정규화 객체 → derbyMessage + **명확한 [다시 시도]**(query refetch) + 장식용 [더비 위로하기](햅틱+토스트).
- 루트 `ErrorBoundary`: 렌더 크래시 → "더비가 화면을 떨어뜨렸습니다" + 앱 재시작.

---

## 4. 상태 관리

| 상태 | 도구 |
|---|---|
| 서버 데이터 전부 | TanStack Query (남은 채팅 횟수조차 Query 캐시가 단일 소스) |
| 인증 라이프사이클 | Zustand `authStore` (토큰 원본은 SecureStore) |
| 기기 로컬 영속 플래그 | Zustand `appStateStore` + persist(AsyncStorage) |
| 휘발성 UI | Zustand `uiStore` |
| 구독 상태 | Query `['billing']` + RC listener |

**금지: 서버 데이터를 Zustand에 복사하지 않는다.**

```ts
// QueryClient 기본값
{ staleTime: 60_000, gcTime: 30 * 60_000,
  retry: (count, err) => !isDerbyApiError(err, 4) && count < 2,   // 4xx 재시도 금지
  refetchOnWindowFocus: false }                                    // AppState 'active' → focusManager 연결
```

캐시 정책 요약: home 60s / tasks·today 5m(날짜 키) / chat·quota 30s / chat·messages Infinity(append only) / diary 2m / settings 5m / billing은 RC listener + sync가 setQueryData.

낙관적 업데이트는 **일기 삭제에만**. 더비 응답이 필요한 액션(반응·채팅)은 서버 응답을 기다리고 로딩 개그로 마스킹 — **레이턴시가 콘텐츠가 되는 구조.**

---

## 5. API 클라이언트 (`src/api/client.ts`)

SecureStore 키: `dubby.accessToken`(30일), `dubby.deviceId`(최초 실행 시 UUID 생성 — 게스트 계정의 앵커). refresh 토큰 없음.

```ts
async function request<T>(path: string, init?: RequestInit): Promise<T>
```

책임:
1. baseURL + `/api/v1`, `Authorization: Bearer`, `Accept-Language` 주입.
2. **401 처리:** 저장된 deviceId로 `POST /auth/device` 재호출(진행 중 Promise 공유 뮤텍스 — 동시 다발 방지) → 토큰 갱신 → 원 요청 1회 재시도.
3. **에러 정규화:** 모든 실패를 `DerbyApiError { status, code, message, derbyMessage }`로 throw. 네트워크 단절은 `code: 'NETWORK'` + `theme/copy.ts` 폴백 문구("더비가 서버에게 말을 걸었지만 무시당했습니다").
4. **표시 규칙:** Query 에러 → 화면 `DerbyErrorView` / Mutation 에러 → 전역 MutationCache `onError`에서 `DerbyToast` / 특수 코드(`CHAT_LIMIT_EXCEEDED` 등)는 화면에서 개별 catch·라우팅.
5. **장난 금지 구역:** `BILLING_*`, `AUTH_*` 코드는 derbyMessage 대신 정자세 안내 우선.

---

## 6. 푸시 알림 (수신 측)

### 6.1 권한 흐름

```text
온보딩 마지막 스텝 → 프리 프롬프트(더비 톤 인앱 다이얼로그)
  [허락] → Notifications.requestPermissionsAsync()
            granted → 토큰 등록 / denied → 재요청 안 함(설정 화면 안내만)
  [나중에] → 홈 3회 방문 후 1회만 재프롬프트 (appStateStore.lastPushPromptAt)
Android: 권한 요청 전 setNotificationChannelAsync('default') 선행
```

### 6.2 토큰 등록
`getExpoPushTokenAsync({ projectId })` → `POST /push/tokens { expoPushToken, deviceId, platform }`. **앱 시작 시마다 호출(멱등)** — 토큰 로테이션 대응.

설정 알림 OFF = OS 권한 회수가 아니라 서버 발송 중단(`PUT /push/settings { enabled: false }`). 이스터에그 다이얼로그 1회 후 **확실히 반영.**

### 6.3 딥링크 라우팅

payload: `data: { pushLogId, deeplink: "dubby://tasks" }`.

- 수신 탭: `addNotificationResponseReceivedListener` + 콜드 스타트 `getLastNotificationResponseAsync()` 양 경로. 콜드 스타트는 인증/온보딩 가드 완료 후 소비하도록 `uiStore.pendingDeepLink`에 보관.
- 탭 시 `POST /push/logs/{pushLogId}/open` fire-and-forget (클릭률 지표).
- 외부 딥링크(`dubby://tasks/123`)도 scheme으로 동일 라우트 도달.

---

## 7. RevenueCat 연동

```ts
// 인증 완료 후 1회
Purchases.configure({ apiKey: Platform.select({ ios: RC_IOS_KEY, android: RC_ANDROID_KEY }),
                      appUserID: userId });   // 서버 발급 UUID — 웹훅 app_user_id와 1:1
```

- 동기화 이중 경로: 서버(웹훅 — 진실의 원천) + 클라(`addCustomerInfoUpdateListener` → setQueryData, 구매 직후 `POST /billing/sync`).
- `usePremium()`: `customerInfo.entitlements.active['salary']` — **UI 게이팅만.** 실제 기능 제한은 항상 서버 판정.
- Paywall 진입점: 홈 메뉴 / 과로 화면 커피 버튼 / 설정 프리미엄 행.

---

## 8. 더비 캐릭터 표현

### 8.1 비주얼 — 정적 PNG 표정 세트 (MVP 채택)
`src/assets/derby/{mood}.png` × 8종: `idle, confident, thinking, panic, collapsed, happy, sad, sleeping`. 서버 `derby.mood`와 매핑.
"살아있음"은 Reanimated로: idle 시 2~4초 주기 미세 상하 부유 + 가끔 기울어짐 — 이미지 1장으로 충분히 하찮게 움직인다. Lottie는 post-MVP 스킨에서 검토. 스킨 시스템 대비 경로 구조만 `{skinId}/{mood}.png`로 잡아둔다.

### 8.2 DerbyLoading
1. `theme/copy.ts` 로딩 스크립트 세트(페르소나 §11.2, 세트당 5~8줄)에서 랜덤 선택, 700~1200ms 간격 교체 + 가짜 퍼센트.
2. **99% 개그는 1.5초 상한, 실제 응답 도착 시 즉시 종료** — 실 대기 시간을 늘리지 않는다. 응답이 빠르면 최소 노출 400ms만 보장(깜빡임 방지).
3. 설정 `loadingNonsense` OFF → 평범한 스피너.

### 8.3 DerbyToast
하단 더비 아바타 소형 + 말풍선, 전역 1큐. Mutation 코멘트/이스터에그 반응/저장 확인 모두 이 채널.

### 8.4 설정 이스터에그 구현 계약

**공통 규칙(기획 §14.3): 항상 "1회 장난 → 즉시 정상 동작". 사용자의 선택을 두 번 막는 코드 금지.** `EasterEggSwitch`의 onFirstAttempt 콜백이 1회만 호출되는 구조로 강제.

| 이스터에그 | 구현 |
|---|---|
| RunawayButton ("더비가 똑똑해졌다고 믿기" OFF) | Reanimated 스프링으로 좌우 1회 회피 → **두 번째 탭은 무조건 정상 동작.** 세션당 1회(`uiStore.easterEggFired`) |
| 알림 OFF 분리 불안 | 커스텀 다이얼로그 1회 "[그래도 끄기][더비를 안심시키고 유지]" → [그래도 끄기] 즉시 반영 |
| 가짜 설정 항목 | `FakeSettingRow` — 토글 시 토스트만, 상태는 `prefs`에 저장(재설치에도 유지되는 개그) |

### 8.5 공유 (`ShareCard`)
- off-screen 1080×1350(4:5) 고정 렌더 → `react-native-view-shot` 캡처 → `expo-sharing` OS 시트. 하단 더비 로고+앱 이름 워터마크(바이럴 귀속).
- 업무 카드·일기 동일 컴포넌트 재사용. 캡처 실패 시 텍스트 공유 폴백(`Share.share(shareText + 스토어 링크)`).
- 시트 오픈 기준으로 `POST .../share` fire-and-forget.

---

## 9. 테마 — '의도된 하찮음' 디자인 시스템

> 조잡함은 장식 레이어에만. 레이아웃 그리드, 터치 타깃(44pt+), 텍스트 대비(WCAG AA), 내비게이션 일관성은 정상 앱 기준. "못 만든 척"은 색·말투·일러스트·마이크로카피가 담당한다.

```ts
export const colors = {
  background: '#FDF6E3',   // 바랜 서류 미색 — "낡은 사무실"
  surface:    '#FFFFFF',
  ink:        '#2B2B2B',
  inkSub:     '#6E6A5E',
  derbyBlue:  '#4A6CFA',   // 과하게 자신만만한 파랑
  accident:   '#E8503A',   // 사고/경고
  praise:     '#3AA655',
  tapeYellow: '#FFD84D',   // 포스트잇 노랑
  border:     '#2B2B2B',
};
export const radii = { card: 14, button: 10 };
export const spacing = (n: number) => n * 4;
```

- 타이포: 본문은 시스템 폰트(가독성 절대 사수). **더비 발화 전용 폰트 1종**(둥근 손글씨 계열 무료 폰트, expo-font)만 커스텀 — "시스템 UI = 정상, 더비 발화 = 하찮음"의 시각적 분리가 핵심 장치. 가격/약관/에러 복구 버튼은 반드시 시스템 폰트(장난 금지 신호).
- `JankyButton`: 2px 잉크 외곽선 + 오프셋 하드 섀도 + 랜덤 ±1° 기울기(마운트 시 고정) + 탭 햅틱. `JankyCard`: 마스킹테이프 장식 + ±0.5° 기울기.
- **기울기·장식 난수는 항목 id 시드로 고정** — 리렌더마다 흔들리면 진짜 버그처럼 보인다. 하찮음은 결정적이어야 한다.
- 빈 화면은 반드시 더비 아바타 + 상황극 문구 (빈 상태도 콘텐츠).
- 다크모드 MVP 미지원 — 설정에 "다크모드: 더비가 어둠을 무서워해서 준비 중입니다"로 세계관 포장.
