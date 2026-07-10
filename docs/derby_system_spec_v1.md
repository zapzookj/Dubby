# 더비(Dubby) 시스템 스펙 v1

> **문서 목적:** 더비 백엔드(dubbyServer)와 시스템 전반의 구현 기준서.
> 기획안(derby_product_plan_v1), 페르소나 바이블(derby_persona_bible_v1)을 구현 가능한 스펙으로 확정한 문서다.
> API 계약·DB 스키마·에러 코드의 **단일 진실 소스(SSOT)**이며, 다른 문서(LLM 파이프라인, 모바일 아키텍처, 로드맵)는 이 문서를 참조한다.
>
> **버전:** v1.0 (2026-07-10 승인)

---

## 0. 설계 원칙 (전 문서 공통)

1. **표면은 하찮고, 구조는 탄탄하게.** 조잡함은 문구·비주얼 레이어에만 허용한다. 데이터 정합성, 멱등성, 에러 복구는 정상 앱 기준을 지킨다.
2. **템플릿 우선, LLM 최소화.** 실시간 LLM 호출 지점은 ① 자유 채팅(일기 후보 합승) ② 프리미엄 일기 재생성 ③ 프리미엄 업무 후속반응 — 3곳뿐. 나머지는 전부 템플릿.
3. **튜닝 대상 상수는 전부 yml.** 하루 채팅 한도, 푸시 횟수, 오늘의 업무 개수, 쿨다운, TTL, 슬롯 수 등 **운영 중 튜닝 가능성이 있는 모든 수치는 코드에 하드코딩하지 않고 `application.yml`의 `dubby.*` 네임스페이스에 모아 `@ConfigurationProperties`로 주입한다.** 본 문서에 등장하는 수치는 전부 "초기값"이며 확정값이 아니다. (§3 상수 총람 참조)
4. **프롬프트는 코드가 아니라 리소스.** 시스템 프롬프트는 `resources/prompts/` 아래 버전 폴더의 마크다운 파일로 관리하고, 반복 튜닝을 전제로 설계한다. (상세: derby_llm_pipeline_v1.md)
5. **장난 금지 구역.** 결제, 인증, 계정/데이터 삭제, 안전(고위험 입력) 관련 응답·화면은 명확성이 유머보다 우선한다.
6. **1인 + 에이전트 개발 규모 고정.** 단일 서버 + 단일 PostgreSQL. Redis, 메시지 큐, MSA, 분산 락 도입 금지. 규모 문제는 실제로 발생한 뒤에 푼다.

---

## 1. 시스템 아키텍처

### 1.1 구성도

```text
┌─────────────────────────────┐        ┌──────────────────────────────────────┐
│ dubbyMobile                 │  HTTPS │ dubbyServer                          │
│ React Native + Expo + TS    │ ─────► │ Spring Boot 4.1 / Java 17            │
│ - Expo Router               │ /api/v1│ - REST JSON + JWT                    │
│ - TanStack Query / Zustand  │        │ - 템플릿 선정/쿨다운 엔진             │
│ - Expo Notifications(수신)  │        │ - 채팅 쿼터/LLM 중계                  │
│ - RevenueCat SDK            │        │ - 푸시 발송 배치(@Scheduled)          │
└─────────────────────────────┘        │ - RevenueCat Webhook                 │
                                       └───┬──────────┬──────────┬───────────┘
                                           │          │          │
                                     PostgreSQL   OpenRouter   Expo Push API
                                     (+Flyway)    (LLM 유일 창구) (발송/영수증)
                                           ▲
                                RevenueCat Webhook ──┘ (결제 이벤트)
```

- 클라이언트는 OpenRouter를 **절대 직접 호출하지 않는다**. 모든 LLM 트래픽은 서버 경유.
- 푸시 발송은 전적으로 서버 책임. 클라이언트는 토큰 등록 + 수신 + 딥링크 라우팅만.

### 1.2 레포 구조

```text
dubby/                        # 루트 = 단일 git repo (dubbyServer/.git은 제거·흡수)
├── dubbyServer/              # Spring Boot 4.1 + PostgreSQL
├── dubbyMobile/              # React Native + Expo + TypeScript
├── tools/
│   └── seed/build_seed.mjs   # docs 마크다운 → 템플릿 시드 SQL 생성기 (Node)
├── docs/                     # 기획/스펙 문서 (본 문서 포함)
└── CLAUDE.md
```

---

## 2. 백엔드 구조

### 2.1 패키지 구조

레이어드 + 도메인 수직 분할. 루트 패키지는 기존 `com.spring.dubbyserver` 유지.

```text
com.spring.dubbyserver
├── DubbyServerApplication.java
├── global
│   ├── config/        SecurityConfig, JpaConfig, SchedulingConfig, RestClientConfig,
│   │                  DubbyProperties(@ConfigurationProperties("dubby") — 상수 총람 바인딩)
│   ├── security/      JwtProvider, JwtAuthenticationFilter, AuthUser
│   ├── error/         ErrorCode, DerbyCopy, DubbyException, ErrorResponse, GlobalExceptionHandler
│   └── common/        BaseTimeEntity, CursorPageResponse
├── domain
│   ├── user/          AuthController(/auth/device), SettingsController, UserService, User
│   ├── home/          HomeController, HomeService (자체 엔티티 없음 — 조합 전용)
│   ├── template/      Template, TemplateRepository, TemplatePicker(선정/쿨다운 로직 단일 지점),
│   │                  DefaultReactionPool(derby-defaults.yml 로더)
│   ├── task/          TaskController, TaskService, DailyTaskAssignment
│   ├── chat/          ChatController, ChatService, ChatQuotaService, ChatMessage, ChatDailyUsage
│   ├── diary/         DiaryController, DiaryService, DiaryCandidate, DiaryEntry
│   ├── push/          PushController, PushSettingService, PushDispatchScheduler,
│   │                  PushReceiptScheduler, PushToken, PushSetting, PushSendLog
│   ├── billing/       RevenueCatWebhookController, BillingController, BillingService,
│   │                  PurchaseEntitlement, OneTimePurchase, RevenueCatEvent
│   └── metrics/       AppEvent, UserActivityRecorder(인터셉터), AdminMetricsController
└── infra                     # 외부 시스템 클라이언트 (도메인 로직 금지)
    ├── llm/           OpenRouterClient, ModelRouter, PromptFactory, SafetyFilter,
    │                  OutputValidator, LlmBudgetService, LlmUsageLogService
    └── expo/          ExpoPushClient
```

원칙:
- `domain.*`은 `infra.*` 인터페이스에만 의존, `infra`는 도메인을 모른다.
- 템플릿 선정(쿨다운·시간창·프리미엄 필터)은 `TemplatePicker` 한 곳에만 존재하고 task/push가 공용.

### 2.2 Gradle 의존성 (Spring Boot 4.1 기준)

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'

    // 영속성
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'   // Flyway 10+ DB별 모듈 필수

    // 보안 + JWT
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-gson:0.12.6'   // ⚠ Boot 4는 Jackson 3 기본 → jjwt-jackson(Jackson 2 전제) 대신 gson 어댑터 사용

    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-restclient'   // OpenRouter/Expo 호출
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'

    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

**추가하지 않는 것(과잉 방지):** Redis, Kafka, QueryDSL, MapStruct, WebFlux, ShedLock(단일 인스턴스), springdoc(Boot 4 안정화 전).

---

## 3. 설정 관리 — 상수 총람 (`dubby.*`)

**모든 튜닝 대상 상수는 이 네임스페이스가 유일한 소스다.** 코드에서는 반드시 `DubbyProperties` 주입으로 읽는다. DB 기반 설정 테이블(app_settings)은 만들지 않는다 — 1인 운영에서 "yml 수정 + 재배포"가 곧 운영 콘솔이다.

```yaml
# application.yml (공통)
spring:
  application.name: dubbyServer
  jpa:
    hibernate.ddl-auto: validate        # 스키마 변경은 오직 Flyway
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration

dubby:
  auth:
    jwt-secret: ${JWT_SECRET}
    access-token-ttl: 30d               # refresh 토큰 없음 (§4)
  user:
    nickname-max-length: 12
    timezone-change-min-interval: 24h   # 쿼터 리셋 악용 방지 가드
  task:
    daily-count: 3                      # 오늘의 업무 노출 개수
    retry-max: 2                        # RETRY 반응 최대 횟수
    default-cooldown-days: 60           # 시드 파서 기본값
  chat:
    daily-limit: { free: 3, supporter: 20, salary: 50 }
    max-content-length: 500
    history-max-messages: 10            # 컨텍스트 포함 메시지 수 (user+derby 합산)
    per-user-concurrency: 1
  diary:
    slot-limit: { free: 30, salary: 200 }
    candidate-ttl: 72h
    daily-candidate-max: 5              # 하루 일기 후보 생성 상한
    candidate-min-gap-messages: 2       # 후보 생성 간 최소 사용자 메시지 수
    rewrite-daily-limit: 3              # 프리미엄 일기 재생성
  push:
    default-daily-count: 1
    max-daily-count: 3
    default-cooldown-days: 30           # 시드 파서 기본값
    dispatch-interval: 10m              # 발송 폴링 주기
    receipt-interval: 15m               # 영수증 확인 주기
    windows:                            # 유저 로컬 시간 기준
      morning: { start: "08:00", end: "10:00" }
      lunch:   { start: "12:00", end: "13:30" }
      evening: { start: "19:00", end: "21:30" }
    default-slots-by-count:             # maxDailyCount → 발송 슬롯 매핑
      1: [EVENING]
      2: [MORNING, EVENING]
      3: [MORNING, LUNCH, EVENING]
    quiet: { start: "22:00", end: "08:00" }   # 하드 가드 (사용자 설정 아님)
  billing:
    entitlement-id: salary
    products: { salary: dubby_salary_monthly, coffee: dubby_coffee }
    coffee-effect-duration: 24h
  llm:                                  # 상세 구조는 derby_llm_pipeline_v1.md §2
    openrouter:
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENROUTER_API_KEY}
      connect-timeout: 2s
      read-timeout: 25s
    routing:
      chat:          { primary: "openai/gpt-4o-mini", fallbacks: ["google/gemini-2.5-flash-lite"], temperature: 0.9, max-tokens: 600 }
      chat-precise:  { primary: "openai/gpt-4o-mini", fallbacks: ["google/gemini-2.5-flash-lite"], temperature: 0.5, max-tokens: 600 }
      diary:         { primary: "google/gemini-2.5-flash-lite", fallbacks: ["openai/gpt-4.1-nano"], temperature: 0.9, max-tokens: 500 }
      task-followup: { primary: "google/gemini-2.5-flash-lite", fallbacks: ["openai/gpt-4.1-nano"], temperature: 1.0, max-tokens: 240 }
      # ⚠ 모델 ID는 SPIKE-A(페르소나 검증)에서 확정. 위 값은 후보 초기값.
    misread-level-weights: { lv1: 60, lv2: 40 }   # 오해 강도 가중 랜덤
    input-token-hard-cap: 4000
    budget:
      global-daily-token-limit: 10000000          # 전체 일일 캡 (운영자 파산 방지)
  revenuecat:
    webhook-secret: ${REVENUECAT_WEBHOOK_SECRET}
```

프로파일:

```text
application.yml          # 공통 + dubby.* 기본값
application-local.yml    # localhost DB, DEBUG 로깅
application-prod.yml     # env 주입 datasource, forward-headers, actuator health만 노출
```

시크릿 4종은 전부 환경변수: `JWT_SECRET`, `OPENROUTER_API_KEY`, `REVENUECAT_WEBHOOK_SECRET`, `DB_PASSWORD`(+prod `DB_URL`/`DB_USERNAME`). yml 실값·git 커밋 금지. 로컬은 `.env`(gitignore) + IDE run config.

더비 톤 기본 문구(버튼 후속반응 풀, 폴백 문구 등) 역시 별도 리소스 `derby-defaults.yml`로 관리 (§8.3).

---

## 4. 인증 — 익명 디바이스 게스트

### 4.1 정책

| 항목 | 결정 |
|---|---|
| 계정 모델 | 디바이스 설치 시 자동 발급되는 익명 게스트. 소셜 로그인은 post-MVP(P3) |
| 토큰 | **accessToken(JWT HS256) 단일 체계, TTL 30일. refresh 토큰 없음** |
| 근거 | deviceId만 알면 언제든 같은 계정으로 재로그인되는 구조 → 실질 자격증명은 deviceId 자체. 회전/재사용 탐지는 보안 이득 없이 구현량만 늘어 제거 |
| 만료/401 시 | 클라이언트가 동일 deviceId로 `POST /auth/device` 재호출 → 같은 계정 복귀 (데이터 유지) |
| JWT claims | `sub`(userId UUID 문자열), `did`(deviceId), `iat`, `exp` |

### 4.2 시퀀스

```text
[앱 최초 설치]
Client: SecureStore에서 deviceId 조회 → 없으면 UUID 생성/저장
Client → Server: POST /auth/device { deviceId, platform, timezone, locale, appVersion }
Server: users에서 device_id 조회 → 없으면 INSERT (게스트 생성)
Server → Client: { userId, isNewUser, accessToken, expiresIn }
Client: 토큰 SecureStore 저장, RevenueCat SDK Purchases.logIn(userId)

[일반 요청] Authorization: Bearer {accessToken} → JwtAuthenticationFilter → SecurityContext에 userId

[만료 시] 401 AUTH_TOKEN_EXPIRED → 클라가 /auth/device 재호출(뮤텍스로 동시 갱신 방지) → 원 요청 재시도
```

### 4.3 SecurityConfig 요지

```java
http.csrf(csrf -> csrf.disable())
    .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
    .authorizeHttpRequests(a -> a
        .requestMatchers("/api/v1/auth/**", "/api/v1/webhooks/revenuecat",
                         "/api/v1/health", "/actuator/health").permitAll()
        .anyRequest().authenticated())
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

인증 실패도 공통 에러 포맷을 지키도록 `AuthenticationEntryPoint` 커스텀 (`{ code: "AUTH_TOKEN_INVALID", ... }` JSON 직접 write).

---

## 5. REST API 명세 (SSOT)

공통 규칙:
- prefix `/api/v1`, `application/json; charset=utf-8`
- 인증 예외 경로: `/auth/**`, `/webhooks/revenuecat`, `/health`, `/actuator/health`
- 목록은 커서 페이지네이션: `{ items, nextCursor, hasNext }`
- 에러 공통 포맷(§6): `{ code, message, derbyMessage }`
- 공통 에러(전 엔드포인트): `AUTH_TOKEN_INVALID`(401), `AUTH_TOKEN_EXPIRED`(401), `COMMON_INVALID_REQUEST`(400), `COMMON_NOT_FOUND`(404), `COMMON_INTERNAL_ERROR`(500) — 아래 표에서는 생략

### 5.1 Auth

#### `POST /auth/device` (public, 멱등)

```json
// 요청
{ "deviceId": "3f9a2c9e-...", "platform": "IOS", "timezone": "Asia/Seoul", "locale": "ko-KR", "appVersion": "1.0.0" }
// 응답 200
{ "userId": "b7e3...uuid", "isNewUser": true, "accessToken": "eyJ...", "expiresIn": 2592000 }
```

| 에러 | HTTP |
|---|---|
| AUTH_INVALID_DEVICE_ID (UUID 형식 아님) | 400 |
| AUTH_INVALID_TIMEZONE (IANA 아님) | 400 |

### 5.2 Home

#### `GET /home` — 홈 화면 1회 호출 구성

```json
{
  "derby": {
    "mood": "confident",                  // 아바타 표정 매핑용: idle|confident|thinking|panic|collapsed|happy|sad|sleeping
    "statusLine": "근거 없는 자신감",       // HOME_STATUS 템플릿, (userId, 날짜) 시드 고정 — 같은 날 재진입 시 동일
    "accuracy": "112%",                   // (userId, 날짜) 시드 랜덤 87~142%
    "currentWork": "사용자를 돕는 척하기"
  },
  "todayTasks": { "date": "2026-07-10", "total": 3, "reactedCount": 1 },
  "chatQuota": { "tier": "FREE", "limit": 3, "used": 1, "remaining": 2 },
  "diary": { "totalEntries": 12, "pendingCandidates": 1 },
  "billing": { "tier": "FREE", "expiresAt": null }
}
```

`mood`는 저장하지 않고 서버가 계산(일자 시드 + 상태: 쿼터 소진 시 collapsed 등). 스킨/능력치 테이블은 MVP 제외.

### 5.3 Tasks — 오늘의 업무

#### `GET /tasks/today`
유저 로컬 날짜 기준 오늘 배정분. 없으면 **이 시점에 lazy 배정**(§9).

```json
{
  "date": "2026-07-10",
  "tasks": [
    {
      "assignmentId": 5501,
      "templateCode": "TASK-004",
      "title": "회의록 요약을 완료했습니다.",
      "conclusion": "314줄의 회의록을 1058줄로 성공적으로 요약했습니다.",
      "note": "핵심을 놓치지 않기 위해 전부 늘렸습니다.",
      "buttons": [ { "key": "PRAISE", "label": "칭찬하기" }, { "key": "SCOLD", "label": "혼내기" },
                   { "key": "RETRY", "label": "다시 시키기" }, { "key": "IGNORE", "label": "모른 척하기" } ],
      "reaction": null, "retryCount": 0, "saved": false
    }
  ]
}
```

#### `POST /tasks/{assignmentId}/reaction`
`{ "reaction": "PRAISE" }` — 허용값 `PRAISE|SCOLD|RETRY|IGNORE`. **덮어쓰기 허용**(마지막 반응 저장), RETRY만 `dubby.task.retry-max` 초과 시 고정 문구("더비가 이 업무에서 손을 뗐습니다. 전략적 후퇴입니다.") 반환 후 거부.

```json
// 응답 200
{ "assignmentId": 5501, "reaction": "PRAISE", "followUpMessage": "역시 저는 가능성이 있습니다. 아직 가능성만 있습니다." }
```

후속 문구: 템플릿 `content.buttons[key].responses`에서 랜덤 1개, 없으면 `derby-defaults.yml` 기본 풀. 무료=템플릿 고정, SALARY 등급은 LLM 개인화 후속반응(TASK_FOLLOWUP purpose, 실패 시 템플릿 강등 — 사용자는 실패를 모른다).

에러: `TASK_ASSIGNMENT_NOT_FOUND`(404, 타인 소유 포함), `TASK_INVALID_REACTION`(400), `TASK_RETRY_EXHAUSTED`(409).

#### `POST /tasks/{assignmentId}/save`
`{ "saved": true }` → `{ "assignmentId": 5501, "saved": true, "derbyMessage": "저장했습니다. 이 흑역사는 이제 공식 기록입니다." }`

#### `POST /tasks/{assignmentId}/share`
공유 이벤트 기록(지표) + `{ "shareText": "..." }` 반환. 클라 공유 시트 오픈 후 fire-and-forget.

#### `GET /tasks/saved?cursor={id}&size=20`
저장한 업무 목록 (커서 페이지네이션).

### 5.4 Chat — 더비와 협의하기

#### `GET /chat/quota`

```json
{ "tier": "FREE", "limit": 3, "used": 3, "remaining": 0, "resetsAt": "2026-07-11T00:00:00+09:00",
  "exhaustedMessage": "더비가 과로로 쓰러졌습니다. 방금 3개의 문장을 생각하고 모든 에너지를 소진했습니다." }
```

#### `POST /chat/messages`

```json
// 요청 — clientMessageId는 멱등키(UUID, 필수). 재시도 시 이중 차감/이중 과금/중복 저장 방지
{ "clientMessageId": "9d1f...uuid", "content": "이 문장을 영어로 번역해줘: 대표님, 요청하신 커피 가져왔습니다." }
```

처리 순서(상세: derby_llm_pipeline_v1.md §1.3):
① 입력 검증(1~`max-content-length`자) → ② `(user_id, client_msg_id)` 중복이면 기존 응답 그대로 반환(차감 없음) → ③ **SafetyFilter.preCheck — 감지 시 LLM 미호출·쿼터 미차감·대화 미저장으로 SAFETY_NOTICE 즉시 반환** → ④ 쿼터 원자적 선차감(§10) → ⑤ OpenRouter 호출(일기 후보 합승) → ⑥ 모델 자기신고 safety 후검사(감지 시 SAFETY_NOTICE 대체 + 쿼터 환불) → ⑦ 출력 검증(OOC) → ⑧ 저장 + 응답.

```json
// 응답 200 (일반)
{
  "kind": "DERBY",
  "userMessage":  { "id": 9001, "role": "USER",  "content": "...", "createdAt": "2026-07-10T21:03:11+09:00" },
  "derbyMessage": { "id": 9002, "role": "DERBY", "content": "CEO, I have captured the requested coffee.\n\n커피가 도망갈 가능성을 고려했습니다.", "createdAt": "2026-07-10T21:03:13+09:00" },
  "diaryCandidate": { "candidateId": 301, "preview": "사용자님은 커피를 상사에게 배달하는 임무를..." },   // 없으면 null
  "quota": { "limit": 3, "used": 2, "remaining": 1 }
}
```

```json
// 응답 200 (고위험 입력 — 더비 응답 대신 앱 시스템 카드. 쿼터 미차감, 대화 미저장)
{
  "kind": "SAFETY_NOTICE",
  "safetyNotice": {
    "category": "SELF_HARM",
    "title": "안내",
    "body": "지금 많이 힘든 상황이라면, 더비보다 도움이 되는 곳이 있어요.",
    "resources": [ { "label": "자살예방 상담전화 109 (24시간)", "action": "tel:109" } ]
  },
  "quota": { "limit": 3, "used": 1, "remaining": 2 }
}
```

| 에러 | HTTP | 비고 |
|---|---|---|
| CHAT_LIMIT_EXCEEDED | 429 | body에 `resetsAt`, `paywallHint: "SALARY"` 추가. derbyMessage = 과로 문구 |
| CHAT_CONTENT_TOO_LONG | 400 | |
| CHAT_CONCURRENT_REQUEST | 429 | 사용자당 동시 1건 |
| LLM_UPSTREAM_ERROR | 502 | **쿼터 환불** |
| LLM_TIMEOUT | 504 | **쿼터 환불** |
| LLM_BUDGET_EXHAUSTED | 503 | 글로벌 일일 캡 초과 — "더비 사무실의 전기가 오늘치 다 떨어졌습니다." |

#### `GET /chat/messages?cursor={lastId}&size=30` — 과거 대화 (id 내림차순)

#### `POST /chat/messages/{id}/save`
"웃긴 답변 저장" — `is_saved=true`. 지표(13장 "사용자가 웃긴 답변을 저장하는가") + 보존 정책 예외(§15).

### 5.5 Diary — 더비의 일기장

| 메서드/경로 | 설명 |
|---|---|
| `POST /diary/candidates/{id}/approve` | 후보 승인 → 정식 일기 생성. 201 + entry 반환. 에러: `DIARY_CANDIDATE_NOT_FOUND`(404, 만료 포함), `DIARY_SLOT_FULL`(409, `paywallHint` 포함) |
| `POST /diary/candidates/{id}/reject` | 후보 폐기(status=REJECTED, 재노출 없음) |
| `GET /diary/entries?cursor=&size=20` | 목록. 항목: `{ entryId, fact, interpretation, conclusion, autoSaved, isShared, createdAt }` |
| `GET /diary/entries/{id}` | 단건 |
| `DELETE /diary/entries/{id}` | **즉시 실삭제** (soft delete 아님 — "삭제 요청 시 확실히 삭제"). 응답 derbyMessage = 페르소나 바이블 §9.6 문구 |
| `DELETE /diary/entries` | 전체 삭제. `{ "deletedCount": 12, "derbyMessage": "더비의 일기장이 비워졌습니다..." }` |
| `POST /diary/entries/{id}/share` | 공유 이벤트 기록 + shareText 반환 |
| `POST /diary/entries/{id}/rewrite` | SALARY 전용 일기 재생성(DIARY purpose LLM 1회). `dubby.diary.rewrite-daily-limit`/일 |

후보 흐름: 채팅 합승으로 생성(PENDING, TTL `candidate-ttl`=72h) → 클라 카드 "[적게 두기] [찢기]" → approve/reject. 설정 "더비가 마음대로 일기 쓰기" ON 시 자동 APPROVED(`autoSaved=true`, 기본 OFF). 만료 후보는 일일 배치가 삭제.

### 5.6 Push

| 메서드/경로 | 설명 |
|---|---|
| `POST /push/tokens` | `{ "expoPushToken": "ExponentPushToken[...]", "deviceId": "..." , "platform": "ANDROID" }` — 멱등 upsert. 앱 시작 시마다 호출(토큰 로테이션 대응). 검증: `ExponentPushToken[` 접두 |
| `POST /push/tokens/delete` | `{ "expoPushToken": "..." }` — 토큰 제거 (DELETE body 회피 관행) |
| `GET /push/settings` / `PUT /push/settings` | `{ "enabled": true, "maxDailyCount": 1 }` (1~`max-daily-count`). `enabled=false`면 발송 배치에서 완전 제외 — **장난은 해도 OFF는 존중.** quiet hours는 서버 고정 가드로 사용자 설정 아님 |
| `POST /push/logs/{pushLogId}/open` | 푸시 탭 시 클라 호출 → `opened_at` 기록 (클릭률 지표) |

### 5.7 Settings

#### `GET /settings` / `PATCH /settings` (부분 갱신)

```json
{
  "nickname": null,                      // nullable, 최대 dubby.user.nickname-max-length자. 표시 폴백은 "사용자님"
  "timezone": "Asia/Seoul",
  "prefs": { "believeSmarter": true, "dangerousConfidence": true, "loadingNonsense": true, "autoDiary": false }
}
```

- `prefs`는 users.prefs JSONB 한 컬럼 — 이스터에그 토글 키 추가에 유연. **저장된 설정은 실제로 반영되어야 한다**(기획 §14.3).
- 타임존 변경은 `timezone-change-min-interval`(24h)당 1회 제한 — 로컬 자정 리셋(채팅 쿼터) 악용 방지.
- 에러: `SETTINGS_INVALID_TIMEZONE`(400), `SETTINGS_NICKNAME_TOO_LONG`(400), `SETTINGS_TIMEZONE_CHANGE_TOO_OFTEN`(429).

### 5.8 Billing

#### `POST /webhooks/revenuecat` (public, 헤더 시크릿 인증)

- 검증: `Authorization: Bearer {REVENUECAT_WEBHOOK_SECRET}` 일치. 불일치 → 401.
- **멱등+원자 처리:** `revenuecat_events(event_id UNIQUE)` insert와 entitlement 갱신을 **같은 트랜잭션**으로 묶는다. 중복 event_id → 200 즉시 반환. 처리 중 예외 → **500 반환**(트랜잭션 롤백, RevenueCat 재시도에 위임 — "이벤트 저장됐는데 반영 안 됨" 유실 경로 차단).
- **순서 역전 가드:** entitlement 갱신 시 `event_timestamp_ms`를 `last_event_at`에 저장하고, 이보다 오래된 이벤트는 무시(last-write-wins).

| event.type | 처리 |
|---|---|
| INITIAL_PURCHASE, RENEWAL, UNCANCELLATION, PRODUCT_CHANGE (entitlement `salary`) | tier=SALARY, expires_at 갱신 |
| CANCELLATION | will_renew=false (만료 전까지 SALARY 유지) |
| EXPIRATION | tier=FREE 전환 |
| NON_RENEWING_PURCHASE (product `dubby_coffee`) | one_time_purchases insert, `effect_expires_at = now + coffee-effect-duration` |

`app_user_id` = 우리 users.id(UUID 문자열). 클라이언트가 `Purchases.logIn(userId)`로 고정.

#### `GET /billing/me`

```json
{ "tier": "SALARY", "chatDailyLimit": 50, "expiresAt": "2026-08-10T00:00:00Z", "willRenew": true, "coffeeActiveUntil": null }
```

#### `POST /billing/sync`
구매 직후 클라 트리거 — 서버가 RevenueCat REST API로 customer 조회 후 entitlement 갱신(웹훅 지연 대비 벨트+멜빵). 응답 = `GET /billing/me`와 동일.

등급 판정(`BillingService.resolveTier`, 단일 지점): SALARY(entitlement 활성) > SUPPORTER(`coffee effect_expires_at > now`) > FREE.

### 5.9 User 삭제

#### `DELETE /users/me`
계정 + 전체 데이터 삭제 (iOS 심사 필수 요건). 처리: `users.status='DELETING'` 마킹(진행 중 요청 차단) → `DELETE FROM users` → 자식 테이블 전부 `ON DELETE CASCADE`. 예외: `revenuecat_events`는 보존(user FK 없음, 결제 감사 대응 — 잔존 식별자는 UUID뿐). RevenueCat customer 삭제 API 호출은 베스트에포트. **응답 문구는 장난 금지 구역** — 명확한 완료 안내만.

### 5.10 기타

- `GET /health` (public): `{ "status": "ok", "derbyMessage": "더비가 살아있는 척에 성공했습니다." }`
- `GET /admin/metrics/daily` (local/관리용): 일일 LLM 비용·채팅 수·푸시 발송 수 집계.
- `POST /admin/push/test` (local 프로파일 전용): 강제 발송 — 푸시 개발 검증용.

---

## 6. 공통 에러 처리

### 6.1 응답 포맷

```json
{ "code": "DIARY_ENTRY_NOT_FOUND", "message": "Diary entry not found: 771", "derbyMessage": "그 기억은 더비에게 없습니다. 원래 없었을 가능성도 검토 중입니다." }
```

- `code`: 클라이언트 분기용 안정 식별자 (도메인 접두 + SNAKE_UPPER). **클라이언트는 이 문자열로 분기하므로 계약 변경 금지.**
- `message`: 개발자용 영문 (UI 노출 금지).
- `derbyMessage`: UI에 그대로 띄우는 더비 톤 한국어.

### 6.2 derbyMessage 매핑

- `DerbyCopy`: `EnumMap<ErrorCode, String[]>` — 코드당 1~3개 문구, 랜덤 1개 선택(반복 완화). DB 조회 없음.
- **장난 금지 구역**: 결제(`BILLING_*`), 인증(`AUTH_*`), 계정 삭제 관련 코드는 명확한 안내문 1개만. 예: `AUTH_TOKEN_EXPIRED` → "로그인이 만료되었습니다. 앱을 다시 시작하면 자동으로 연결됩니다."

### 6.3 GlobalExceptionHandler

| 핸들러 | 결과 |
|---|---|
| DubbyException | ErrorCode 그대로 |
| MethodArgumentNotValidException | COMMON_INVALID_REQUEST + 첫 필드 오류 |
| HttpMessageNotReadableException | COMMON_INVALID_REQUEST |
| NoResourceFoundException | 404 COMMON_NOT_FOUND |
| Exception (그 외) | 500 COMMON_INTERNAL_ERROR + ERROR 로그(스택은 로그만) |

429 등 부가 필드는 `ErrorResponse` 상속 확장(`ChatLimitErrorResponse { resetsAt, paywallHint }`).

---

## 7. DB 스키마 (PostgreSQL 16, Flyway)

### 7.1 Flyway 구성

```text
src/main/resources/db/migration/
├── V1__init_schema.sql      # 전체 테이블 + 인덱스 (스펙 확정 상태이므로 한 번에 생성)
└── R__seed_templates.sql    # 생성물(tools/seed/build_seed.mjs) — repeatable, 체크섬 변경 시 자동 재실행
```

`ddl-auto: validate` — 스키마 변경은 오직 Flyway. 이후 변경은 V2, V3...로 증분.

### 7.2 DDL (V1__init_schema.sql)

```sql
-- ═══ 사용자 ═══
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id           VARCHAR(128) NOT NULL UNIQUE,
    nickname            VARCHAR(30),                    -- NULL 허용. 표시 폴백 "사용자님". 길이 검증은 서버(yml)
    locale              VARCHAR(10)  NOT NULL DEFAULT 'ko',
    timezone            VARCHAR(50)  NOT NULL DEFAULT 'Asia/Seoul',   -- IANA. 로컬 자정 리셋 기준
    timezone_changed_at TIMESTAMPTZ,                    -- 변경 빈도 가드
    prefs               JSONB        NOT NULL DEFAULT '{}',           -- 이스터에그/설정 토글
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DELETING')),
    platform            VARCHAR(10)  NOT NULL CHECK (platform IN ('IOS','ANDROID')),
    app_version         VARCHAR(20),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_active_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ═══ 템플릿 (업무/푸시/홈상태 공용 원장) ═══
CREATE TABLE templates (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code               VARCHAR(32)  NOT NULL UNIQUE,    -- 'TASK-001', 'PUSH-014' (docs ID 그대로)
    type               VARCHAR(16)  NOT NULL CHECK (type IN ('DAILY_TASK','PUSH','HOME_STATUS')),
    category           VARCHAR(32)  NOT NULL,           -- §8.2 카테고리 사전
    time_window        VARCHAR(16)  NOT NULL DEFAULT 'ANY' CHECK (time_window IN ('ANY','MORNING','LUNCH','EVENING')),
    intensity          VARCHAR(8)   NOT NULL DEFAULT 'LOW' CHECK (intensity IN ('LOW','MID','HIGH')),
    requires_user_name BOOLEAN      NOT NULL DEFAULT FALSE,   -- true면 content 내 {nickname} 치환자
    is_premium         BOOLEAN      NOT NULL DEFAULT FALSE,
    cooldown_days      SMALLINT     NOT NULL,           -- 시드 기본: DAILY_TASK 60 / PUSH 30 (yml 파서 기본값)
    locale             VARCHAR(8)   NOT NULL DEFAULT 'ko',
    tags               TEXT[]       NOT NULL DEFAULT '{}',
    content            JSONB        NOT NULL,           -- §8.1 타입별 구조
    status             VARCHAR(8)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','RETIRED')),
    content_version    INT          NOT NULL DEFAULT 1, -- 시드 upsert 시 실변경에만 +1
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_templates_pick ON templates (type, status, locale, time_window);

-- ═══ 오늘의 업무 배정 (쿨다운 원장 겸함) ═══
CREATE TABLE daily_task_assignments (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id   BIGINT      NOT NULL REFERENCES templates(id),
    assigned_date DATE        NOT NULL,                 -- 유저 로컬 날짜
    slot          SMALLINT    NOT NULL CHECK (slot BETWEEN 1 AND 3),
    reaction      VARCHAR(16) CHECK (reaction IN ('PRAISE','SCOLD','RETRY','IGNORE')),  -- 마지막 반응
    reacted_at    TIMESTAMPTZ,
    retry_count   SMALLINT    NOT NULL DEFAULT 0,
    saved         BOOLEAN     NOT NULL DEFAULT FALSE,
    shared        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, assigned_date, slot)               -- lazy 배정 동시성 방지 (ON CONFLICT DO NOTHING)
);
CREATE INDEX ix_dta_cooldown ON daily_task_assignments (user_id, template_id, assigned_date DESC);
CREATE INDEX ix_dta_saved ON daily_task_assignments (user_id, created_at DESC) WHERE saved;

-- ═══ 채팅 ═══
CREATE TABLE chat_messages (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role              VARCHAR(10) NOT NULL CHECK (role IN ('USER','DERBY')),
    content           TEXT        NOT NULL,
    model             VARCHAR(80),                      -- DERBY만. 실제 응답 모델(폴백 반영)
    misread_level     SMALLINT,                         -- 적용된 오해 강도 (분석용)
    prompt_tokens     INT,
    completion_tokens INT,
    client_msg_id     VARCHAR(64),                      -- 멱등키 (USER 메시지)
    is_saved          BOOLEAN     NOT NULL DEFAULT FALSE,   -- "웃긴 답변 저장"
    safety_flagged    BOOLEAN     NOT NULL DEFAULT FALSE,   -- 이후 컨텍스트에서 제외
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_chat_client_msg ON chat_messages (user_id, client_msg_id) WHERE client_msg_id IS NOT NULL;
CREATE INDEX ix_chat_user_created ON chat_messages (user_id, created_at DESC);

CREATE TABLE chat_daily_usage (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,                           -- 유저 로컬 날짜 → 리셋 배치 불필요 (§10)
    used_count INT  NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, usage_date)
);

-- ═══ 일기장 ═══
CREATE TABLE diary_candidates (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_message_id BIGINT       REFERENCES chat_messages(id) ON DELETE SET NULL,
    fact              VARCHAR(200) NOT NULL,
    interpretation    VARCHAR(400) NOT NULL,
    conclusion        VARCHAR(200) NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED','EXPIRED')),
    expires_at        TIMESTAMPTZ  NOT NULL,            -- now + dubby.diary.candidate-ttl
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_diary_cand_user ON diary_candidates (user_id, status, created_at DESC);

CREATE TABLE diary_entries (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    candidate_id   BIGINT       REFERENCES diary_candidates(id) ON DELETE SET NULL,
    fact           VARCHAR(200) NOT NULL,
    interpretation VARCHAR(400) NOT NULL,
    conclusion     VARCHAR(200) NOT NULL,
    auto_saved     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_shared      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);  -- 삭제는 즉시 실삭제 (soft delete 컬럼 없음)
CREATE INDEX ix_diary_user_created ON diary_entries (user_id, created_at DESC);

-- ═══ 푸시 ═══
CREATE TABLE push_tokens (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expo_push_token VARCHAR(200) NOT NULL UNIQUE,
    device_id       VARCHAR(128),
    platform        VARCHAR(10)  NOT NULL CHECK (platform IN ('IOS','ANDROID')),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,     -- DeviceNotRegistered 시 false
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_used_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_push_tokens_user ON push_tokens (user_id) WHERE is_active;

CREATE TABLE push_settings (
    user_id         UUID     PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    enabled         BOOLEAN  NOT NULL DEFAULT TRUE,
    max_daily_count SMALLINT NOT NULL DEFAULT 1,        -- 기본값·범위는 yml (default/max-daily-count)
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 발송 이력 (푸시 쿨다운 원장 겸함. 큐 없음 — 판정 즉시 발송+로그)
CREATE TABLE push_send_logs (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id        BIGINT      NOT NULL REFERENCES templates(id),
    slot               VARCHAR(16) NOT NULL CHECK (slot IN ('MORNING','LUNCH','EVENING')),
    local_date         DATE        NOT NULL,
    title              VARCHAR(100) NOT NULL,           -- 발송 시점 스냅샷
    body               TEXT        NOT NULL,
    status             VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','SENT','TICKET_ERROR','DELIVERED','RECEIPT_ERROR')),
    expo_ticket_id     VARCHAR(64),
    error_code         VARCHAR(40),
    sent_at            TIMESTAMPTZ,
    receipt_checked_at TIMESTAMPTZ,
    opened_at          TIMESTAMPTZ,                     -- 클릭률 지표
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, local_date, slot)                  -- 슬롯당 1회 — 중복 발송 구조적 차단
);
CREATE INDEX ix_psl_cooldown ON push_send_logs (user_id, template_id, local_date DESC);
CREATE INDEX ix_psl_receipt  ON push_send_logs (status, sent_at) WHERE status = 'SENT';

-- ═══ 결제 ═══
CREATE TABLE purchase_entitlements (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entitlement   VARCHAR(40)  NOT NULL,                -- 'salary'
    status        VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE','EXPIRED','BILLING_ISSUE','CANCELLED')),
    product_id    VARCHAR(100),
    environment   VARCHAR(20)  NOT NULL DEFAULT 'PRODUCTION',
    will_renew    BOOLEAN      NOT NULL DEFAULT TRUE,
    purchased_at  TIMESTAMPTZ,
    expires_at    TIMESTAMPTZ,
    last_event_at TIMESTAMPTZ,                          -- 이벤트 순서 역전 가드 (last-write-wins)
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, entitlement)
);

CREATE TABLE one_time_purchases (                       -- 커피 등 소모성
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id        VARCHAR(100) NOT NULL,            -- 'dubby_coffee'
    rc_transaction_id VARCHAR(100) NOT NULL UNIQUE,     -- 재전송 멱등
    effect_expires_at TIMESTAMPTZ,                      -- 구매 + coffee-effect-duration
    purchased_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_otp_user_effect ON one_time_purchases (user_id, effect_expires_at DESC);

-- RC 웹훅 원본. user FK 없음 — 계정 삭제 후에도 결제 감사 기록 보존
CREATE TABLE revenuecat_events (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id           VARCHAR(100) NOT NULL UNIQUE,    -- 멱등
    event_type         VARCHAR(60)  NOT NULL,
    app_user_id        VARCHAR(100) NOT NULL,
    event_timestamp_ms BIGINT,
    payload            JSONB        NOT NULL,
    received_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ═══ LLM 사용량 ═══
CREATE TABLE llm_usage_log (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id           UUID REFERENCES users(id) ON DELETE SET NULL,
    purpose           VARCHAR(20) NOT NULL,             -- CHAT|CHAT_PRECISE|DIARY|TASK_FOLLOWUP
    model             VARCHAR(80) NOT NULL,
    prompt_version    VARCHAR(10) NOT NULL,             -- 'v1' — 프롬프트 튜닝 비교용
    misread_level     SMALLINT,
    prompt_tokens     INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    cost_usd          NUMERIC(10,6),
    latency_ms        INT,
    finish_reason     VARCHAR(20),
    safety_category   VARCHAR(20),
    validation_failed BOOLEAN NOT NULL DEFAULT FALSE,   -- OOC 검증 실패 (모델별 OOC율 모니터링)
    fallback_used     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_llm_usage_day ON llm_usage_log (created_at);

CREATE TABLE global_llm_usage (                         -- 전체 일일 캡 카운터 (Redis 불필요)
    usage_date     DATE PRIMARY KEY,                    -- UTC 기준
    total_tokens   BIGINT NOT NULL DEFAULT 0,
    total_cost_usd NUMERIC(10,4) NOT NULL DEFAULT 0
);

-- ═══ 지표 ═══
CREATE TABLE app_events (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     UUID,
    event_type  VARCHAR(48) NOT NULL,                   -- TASK_REACTION, CHAT_SENT, CHAT_LIMIT_HIT, DIARY_SAVED, ...
    target_type VARCHAR(24),
    target_id   BIGINT,
    properties  JSONB NOT NULL DEFAULT '{}',            -- 자유 텍스트(채팅/일기 본문) 금지 — ID 참조만
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_ae_type_time ON app_events (event_type, occurred_at);

CREATE TABLE user_activity_daily (                      -- D1/D7 리텐션 전용
    user_id       UUID NOT NULL,
    activity_date DATE NOT NULL,                        -- 유저 로컬 날짜
    PRIMARY KEY (user_id, activity_date)
);
```

**의도적으로 만들지 않는 것:** skins/user_skins/derby_profiles(P2에서 추가), app_settings(yml 단일화), refresh_tokens(단일 토큰), template_exposures(assignment/send_log가 쿨다운 원장 겸함), push 발송 큐(즉시 발송+로그), 파티셔닝.

---

## 8. 템플릿 시스템

### 8.1 content JSONB 구조

**DAILY_TASK:**

```json
{
  "title": "회의록 요약을 완료했습니다.",
  "conclusion": "314줄의 회의록을 1058줄로 성공적으로 요약했습니다.",
  "note": "핵심을 놓치지 않기 위해 전부 늘렸습니다.",
  "buttons": {
    "PRAISE": { "label": "칭찬하기", "responses": ["역시 저는 가능성이 있습니다.\n아직 가능성만 있습니다."] },
    "SCOLD":  { "label": "혼내기",   "responses": ["혼났습니다.\n반성 기능이 실행되었습니다."] },
    "RETRY":  { "label": "다시 시키기", "responses": ["다시 해봤습니다.\n결과가 더 자신감 있어졌습니다."] },
    "IGNORE": { "label": "모른 척하기", "responses": ["모른 척하신 것을 기록했습니다.\n기록의 용도는 없습니다."] }
  },
  "shareText": "더비의 업무 보고: 314줄의 회의록을 1058줄로 성공적으로 요약했습니다."
}
```

- `buttons` 키는 고정 enum **PRAISE/SCOLD/RETRY/IGNORE** (저장은 반응이 아님 — 별도 API).
- `buttons` 전체 또는 특정 키 생략 가능 → `derby-defaults.yml` 기본 풀 폴백 (§8.3).
- `responses` 배열에서 랜덤 1개 반환.

**PUSH:**

```json
{ "title": "업무 보고", "body": "놀랍게도 제가 사용자님을 대신해 아무것도 안 했습니다. 실수 없이 완수했습니다.", "deeplink": "dubby://tasks" }
```

**HOME_STATUS:**

```json
{ "statusLine": "근거 없는 자신감", "currentWork": "사용자를 돕는 척하기" }
```

로딩/에러/설정 이스터에그/온보딩 문구는 템플릿이 아니라 **클라이언트 번들 하드코딩**(`theme/copy.ts`) — 오프라인·네트워크 에러 상황에서도 떠야 하기 때문. 서버 copy-pack 엔드포인트는 post-MVP.

### 8.2 카테고리 사전 (하루 중복 회피 단위)

| type | category |
|---|---|
| DAILY_TASK | `ANALYSIS`, `SCHEDULE`, `SELF_IMPROVE`, `USER_OBSERVE`, `META_REPORT` |
| PUSH | `HELP_REQUEST`, `INCIDENT_REPORT`, `FAKE_URGENT`, `WORK_DONE`, `SELF_PRAISE`, `SEPARATION_ANXIETY`, `NO_CONTENT` |

5~8개 수준 유지 — 너무 잘게 쪼개면 회피 로직이 무의미해진다.

### 8.3 기본 반응 풀 (`derby-defaults.yml`)

버튼 키별 기본 후속 문구 8~12개(페르소나 바이블 §14.3 시드). 서버 부팅 시 로드, 템플릿에 해당 키 없으면 여기서 랜덤. LLM 폴백 문구 풀, retryExhausted 문구도 이 파일에서 관리 — **문구 튜닝 = 파일 수정 + 재배포.**

### 8.4 시딩 파이프라인 (docs 마크다운 = 콘텐츠 SSOT)

```text
docs/derby_daily_tasks_top30_v2.md ─┐
docs/derby_push_notifications_top30_v1.md ─┤→ tools/seed/build_seed.mjs
(신규 템플릿은 meta 주석 포함)        ┘        └→ dubbyServer/src/main/resources/db/migration/R__seed_templates.sql
```

- 기존 docs 형식(`### TASK-NNN`/`✅`/`결론:`/`비고:`, `## PUSH-NNN`/`**제목:**`/`**본문:**`)을 **수정 없이 파싱**한다. 문서에 없는 메타데이터는 헤딩 아래 HTML 주석으로 선택 기재, 생략 시 파서 기본값(yml의 default-cooldown-days 등):

```markdown
### TASK-031
<!-- meta: category=ANALYSIS, intensity=MID, time_window=ANY -->
```

- 생성 SQL: `INSERT ... ON CONFLICT (code) DO UPDATE`(실변경 시에만 content_version+1). docs에서 제거된 code는 `RETIRED` 처리(물리 삭제 금지 — 이력 FK/지표 보존), 복귀 시 재활성화.
- 파서 검증(실패 시 exit 1): code 중복/형식(`^(TASK|PUSH)-\d{3}$`), category 사전 위반, PUSH body 100자/title 20자 초과(잠금화면 잘림), DAILY_TASK 필수 필드 누락, `{nickname}` 치환자 ↔ requires_user_name 정합.
- 운영 절차: docs 편집 → `node tools/seed/build_seed.mjs` → git diff 검수 → 커밋 → 배포 시 Flyway 자동 반영. **관리자 웹 UI는 만들지 않는다.**
- A/B 변형은 새 code로 추가(TASK-101), code는 불변 식별자.

### 8.5 템플릿 선정 로직 (`TemplatePicker` — task/push 공용)

공통 필터: `type` + `status='ACTIVE'` + `locale` + (`is_premium=false` OR 프리미엄 유저) + **쿨다운**(해당 원장 테이블에서 `cooldown_days` 내 노출 이력 제외) + 시간창(`time_window IN ('ANY', :slot)`, 푸시만).

쿨다운 조회 (오늘의 업무 예):

```sql
AND NOT EXISTS (
    SELECT 1 FROM daily_task_assignments a
    WHERE a.user_id = :userId AND a.template_id = t.id
      AND a.assigned_date > :localDate - t.cooldown_days
)
```

가중치: 저장/공유했던 템플릿은 재노출 가중치 0.3 (기획 §8.2).

---

## 9. 오늘의 업무 — 배정 알고리즘

**배정 시점: 첫 조회 시 lazy 생성.** (자정 배치 없음 — 타임존별 배치 불필요, 활성 유저만 비용 발생, 배치 실패 시 전원 공백 리스크 제거)

```text
1. localDate = LocalDate.now(ZoneId.of(user.timezone))
2. SELECT ... WHERE user_id=? AND assigned_date=?  → dubby.task.daily-count건이면 즉시 반환
3. 없으면 TemplatePicker로 후보 조회 (쿨다운·프리미엄·가중치)
4. 서비스 레이어 선택: 카테고리 중복 회피 + intensity HIGH 하루 1개 제한 + 가중 셔플로 N개 확정
5. INSERT ... ON CONFLICT (user_id, assigned_date, slot) DO NOTHING   ← 동시 요청(더블 탭) 방어
6. 재SELECT 후 반환 (경쟁에서 졌어도 동일 결과)
```

**풀 고갈 완화 (필수):** 템플릿 30개 × 쿨다운 60일이면 약 20일 만에 고갈된다.
① 카테고리 중복 회피 해제 → ② 그래도 부족하면 쿨다운 무시하고 least-recently-shown 순으로 채움. 템플릿이 300개로 늘면 자연히 발동하지 않는다.

---

## 10. 채팅 쿼터 (Rate Limit)

- 한도는 `dubby.chat.daily-limit`(yml) × `BillingService.resolveTier()`. 초기값 FREE 3 / SUPPORTER 20 / SALARY 50.
- `usage_date = LocalDate.now(user.timezone)` — 날짜가 바뀌면 새 row가 시작되므로 **리셋 배치 없음**. (자정 카운터 삭제 스케줄러 금지 — 타임존별 자정이 달라 버그 원인)
- **원자적 선차감** (동시 요청 경쟁 방지):

```sql
INSERT INTO chat_daily_usage (user_id, usage_date, used_count) VALUES (:userId, :usageDate, 1)
ON CONFLICT (user_id, usage_date)
DO UPDATE SET used_count = chat_daily_usage.used_count + 1, updated_at = now()
WHERE chat_daily_usage.used_count < :limit
RETURNING used_count;
```

- 반환 row 없음 → 429 `CHAT_LIMIT_EXCEEDED` (+`resetsAt`, `paywallHint`).
- **환불 규칙:** LLM 실패(502/504), 모델 자기신고 SAFETY_NOTICE 대체 시 `used_count - 1`. 사전 필터 SAFETY_NOTICE는 애초에 차감 전이라 환불 불필요.
- 멱등키: `(user_id, client_msg_id)` 유니크 충돌 시 기존 저장 응답 재반환(차감 없음).

---

## 11. 푸시 발송 시스템

### 11.1 발송 스케줄러 (`PushDispatchScheduler`, `dispatch-interval` 주기 폴링)

타임존별 크론 없이 단일 잡:

```text
1. 대상: push_settings.enabled=true AND 활성 push_token 보유 유저 전체 로드 (토이 규모 — 단일 쿼리)
2. 유저별 판정 (Java에서 타임존 계산):
   - now(user.timezone)가 활성 슬롯 창(yml windows) 안인가
   - quiet hours(22–08) 하드 가드
   - 유저 maxDailyCount에 해당하는 슬롯인가 (default-slots-by-count 매핑)
   - 지터: hash(userId, epochDay) % 창 길이(분) → 매일 다른 시각에 발송
3. 멱등 선삽입: push_send_logs에 (user_id, local_date, slot) PENDING을 ON CONFLICT DO NOTHING으로 insert
   → 이미 있으면 skip. **발송 전 기록이므로 배치 재시작/중복 실행에도 슬롯당 1건 구조 보장**
4. 템플릿 선정 (TemplatePicker): PUSH 타입, 시간창, 30일 쿨다운(push_send_logs 원장),
   같은 local_date에 발송된 category 제외(계열 하루 1개), requires_user_name=true인데 닉네임 없으면 제외.
   후보 없으면 그 슬롯은 건너뛴다 (완화 없음 — 푸시는 공백보다 반복이 문제)
5. Expo 발송 (트랜잭션 밖): POST https://exp.host/--/api/v2/push/send
   - 100건 청크, { to, title, body, data: { pushLogId, deeplink }, sound: "default" }
   - 티켓 ok → SENT + ticket_id / error → TICKET_ERROR
   - **티켓 단계 DeviceNotRegistered → 즉시 push_tokens.is_active=false**
```

### 11.2 영수증 확인 (`PushReceiptScheduler`, `receipt-interval` 주기)

- 대상: `status='SENT'`, 발송 15분 경과, receipt 미확인 → `POST /--/api/v2/push/getReceipts` (1000건 청크).
- ok → DELIVERED / error → RECEIPT_ERROR (+DeviceNotRegistered → 토큰 무효화).
- 48시간 지난 SENT는 DELIVERED 간주 마감(Expo 영수증 보존 기한).

### 11.3 원칙

- 유저가 알림 OFF(`enabled=false`) 시 발송 배치에서 완전 제외 — 이스터에그는 클라 UI 1회뿐, **선택은 확실히 존중.**
- 토큰 무효화 시 `enabled`는 건드리지 않는다 — 재설치 후 새 토큰 등록되면 자동 재개.
- 개별 유저 발송 실패는 catch 후 로그만(다음 주기 자연 재시도 없음 — 슬롯당 1회 원칙 우선).

---

## 12. 결제 (RevenueCat)

### 12.1 상품 정의

| 상품 | RC 식별자 | 유형 | 효과 (수치는 전부 yml) |
|---|---|---|---|
| 더비 월급 | product `dubby_salary_monthly` / entitlement `salary` | 구독 | tier=SALARY: 채팅 50회/일, 일기 슬롯 200, 프리미엄 템플릿·LLM 후속반응·일기 재생성 |
| 더비에게 커피 사주기 | product `dubby_coffee` | 소모성 | 구매 후 24시간 tier=SUPPORTER: 채팅 20회/일 |

- 결제 화면 문구는 뻔뻔하되 **가격/갱신조건/해지 안내는 장난 문구와 시각적으로 분리**(장난 금지 구역).
- "더비 Ultra"(Pro와 차이 없음 개그 상품)는 허위표시 심사 리스크로 MVP 제외.

### 12.2 동기화 경로 (이중)

```text
[진실의 원천 = RevenueCat]
경로 A (서버): RC Webhook → revenuecat_events(멱등) + entitlement 갱신 (같은 트랜잭션, 실패 시 500 → RC 재시도)
경로 B (클라): 구매 성공 직후 POST /billing/sync + CustomerInfoUpdateListener → UI 즉시 반영
기능 제한 판정(채팅 한도 등)은 항상 서버. 클라 usePremium()은 UI 게이팅만.
```

---

## 13. 지표 (MVP 최소셋)

- **서버 발생 이벤트만** `app_events`에 기록: TASK_ASSIGNED/TASK_REACTION/TASK_SAVED/TASK_SHARED/CHAT_SENT/CHAT_LIMIT_HIT/DIARY_SAVED/DIARY_SHARED/PAYWALL 관련은 클라 진입 시점 대신 서버 액션 기준. 클라 이벤트 배치 파이프라인(화면 진입 추적 등)과 외부 애널리틱스는 post-MVP.
- `user_activity_daily`: JWT 인증 인터셉터에서 하루 1회 upsert → D1/D7 리텐션이 조인 두 번으로 산출.
- 푸시 클릭률: `push_send_logs.opened_at / sent`.
- 템플릿 성과: 뷰 2개(`v_task_template_stats` — keep_rate 랭킹, `v_push_template_stats` — open_rate). 운영 판단(월 1회 수동): keep_rate 상위 → 변형 제작, open_rate 하위 10% → RETIRED, SCOLD 압도 업무 → 페르소나 재검수.

---

## 14. 데이터 보존 / 삭제 정책

| 대상 | 정책 |
|---|---|
| 일기 삭제 | **즉시 실삭제** (사용자 고지와 실제 동작 일치 — "삭제 요청 시 확실히 삭제") |
| 일기 후보 | TTL(72h) 경과 시 일일 배치 삭제 |
| 채팅 메시지 | 90일 초과분 일일 배치 삭제. 단 `is_saved=true` 보존 |
| chat_daily_usage / push_send_logs | 90일 초과 행 배치 삭제 |
| daily_task_assignments | 보존 (쿨다운 원장, 행당 수십 byte) |
| 계정 삭제 | `DELETE /users/me` → status='DELETING' 마킹 → CASCADE 즉시 삭제. revenuecat_events만 보존(감사) |

일일 배치는 `@Scheduled` 단일 잡 하나로 통합 (새벽 UTC).

---

## 15. 확정 결정 요약 (교차 검증 68건 해소 기준)

| 항목 | 확정 |
|---|---|
| 사용자 PK | UUID (RC app_user_id 겸용 — 추측 가능 ID 금지) |
| 인증 | /auth/device + 30일 액세스 토큰 단일 (refresh 없음) |
| 반응 enum | PRAISE/SCOLD/RETRY/IGNORE. 저장은 별도. 덮어쓰기 허용, RETRY만 max 2 |
| 채팅 에러코드 | `CHAT_LIMIT_EXCEEDED` (429) |
| 채팅 응답 구분 | `kind: DERBY \| SAFETY_NOTICE` |
| LLM 출력 계약 | `{ reply, misreadType, diary, safety }` |
| 일기 | candidates/entries 분리, TTL 72h, approve/reject, 슬롯 30/200, 실삭제 |
| 푸시 | POST /push/tokens(멀티 토큰), 설정 {enabled, maxDailyCount}, quiet 고정 가드, 기본 저녁 1회 |
| 결제 | entitlement `salary`, tier FREE/SUPPORTER/SALARY, 커피 24h 효과 |
| 템플릿 | content JSONB, type 3종(DAILY_TASK/PUSH/HOME_STATUS), R__ repeatable 시딩 |
| 클라 문구 | 로딩/에러/이스터에그/온보딩 = 클라 하드코딩 |
| 상수 | 전부 `dubby.*` yml (`DubbyProperties`) — 본 문서의 수치는 초기값 |
| JWT 라이브러리 | jjwt + **jjwt-gson** (Boot 4 Jackson 3 혼재 회피) |
| MVP 제외 | 스킨/도감/능력치/소셜 로그인/클라 이벤트 수집/더비 Ultra/다크모드/스트리밍 채팅 |
