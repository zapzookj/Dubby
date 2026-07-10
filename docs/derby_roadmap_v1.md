# 더비(Dubby) 개발 로드맵 v1

> **문서 목적:** MVP 출시까지의 개발 순서·완료 기준·검증 시나리오.
> 스펙 상세는 [derby_system_spec_v1.md](derby_system_spec_v1.md) / [derby_llm_pipeline_v1.md](derby_llm_pipeline_v1.md) / [derby_mobile_architecture_v1.md](derby_mobile_architecture_v1.md) 참조 — 본 문서는 수치·계약을 재정의하지 않는다.
>
> **버전:** v1.0 (2026-07-10 승인)

---

## 0. 로드맵 원칙 — 에이전트 페이스

이 프로젝트는 에이전트 바이브 코딩으로 개발한다. 따라서:

1. **시간 견적 없음.** 사람 기준 주 단위 일정, S/M/L 규모 추정은 쓰지 않는다. 페이즈는 시간이 아니라 **의존성과 검증 게이트**로만 구분한다. 한 페이즈가 하루에 끝나도 되고, 여러 페이즈를 한 세션에 묶어 진행해도 된다.
2. **구현 범위·품질은 그대로.** 빠르게 간다는 것은 스펙을 줄이는 게 아니라, 사람 페이스에 맞춘 완충·세분화를 없앤다는 뜻이다. 각 페이즈의 DoD는 전부 충족해야 다음으로 간다.
3. **진짜 병목은 외부 의존성이다.** 에이전트 속도가 못 줄이는 것: Apple/Google 개발자 계정 승인, 스토어 심사, 실기기 테스트, RevenueCat/스토어 상품 등록 전파. → **P0 시작과 동시에 전부 착수**하고, 코드는 그동안 병렬로 나간다.
4. **검증 게이트는 사람 몫.** 페르소나가 웃긴지(SPIKE-A, P2 회귀), 템플릿이 재미있는지는 사용자(오너)의 판정이 필요하다. 각 페이즈 끝의 "검증 시나리오"가 그 지점이다.
5. **콘텐츠는 병렬 트랙.** 템플릿 30→90개 확장, 카피 작성은 에이전트가 초안 생성 → 오너 검수("진짜 불편한가, 더비답게 어이없는가") 루프로 개발과 병행.

## 0.1 페이즈 맵

```text
P0 파운데이션 ──► P1 템플릿 코어+UX 셸 ──► P2 채팅+LLM+일기장 ──► P3 푸시 ──► P4 수익화 ──► P5 출시
   │
   └─ [P0에서 즉시 착수하는 외부 트랙 — 코드와 무관하게 리드타임이 김]
      · Apple Developer Program / Google Play Console 계정 등록
      · RevenueCat 프로젝트 생성, EAS 프로젝트 생성
      · 더비 캐릭터 표정 PNG 8종 제작(외주/생성)
      · 개인정보처리방침 호스팅 준비

   [상시 병렬 콘텐츠 트랙]
      · 오늘의 업무 템플릿 30→90개, 푸시 템플릿 30→90개 (30일 무중복 분량)
      · 홈 상태 문구 15개, 에러/로딩/이스터에그 카피
      · 확장 기준: 각 템플릿 문서의 "후속 확장 규칙" 5문항 통과
```

핵심 검증 순서는 유지한다: **템플릿만으로 웃긴가(P1) → LLM을 붙이면 더 웃긴가(P2) → 다시 오게 만드는가(P3) → 돈을 낼 만한가(P4)**.

순서의 논리:
- 템플릿 코어가 LLM보다 먼저 — LLM 비용 0원으로 핵심 감정("아니 얘 왜 이래 ㅋㅋ")을 검증할 수 있는 유일한 기능이고, 템플릿 선정/쿨다운 인프라는 푸시가 그대로 재사용한다.
- SPIKE-A(페르소나 재현 검증)는 제품 존폐급 리스크이므로 P0~P1 중 아무 때나, P2 착수 전에만 끝내면 된다.
- 결제가 마지막 — 외부 의존성이 가장 크고, 없어도 재미 검증이 가능하다.

---

## P0. 파운데이션

**목표:** 전체 스키마·인증·공통 인프라가 깔린 서버 + 뼈대 앱이 서로 통신한다.
**선행:** 없음.

### 체크리스트

**[공통]**
- [ ] 루트를 단일 git repo로 초기화, `dubbyServer/.git` 제거·흡수. 루트 `.gitignore`, `CLAUDE.md` 정비
- [ ] 외부 트랙 착수: Apple/Google 개발자 계정, RevenueCat 프로젝트, EAS 프로젝트, 캐릭터 PNG 발주

**[백엔드]**
- [ ] build.gradle 의존성 확장 (스펙 §2.2 — jjwt-gson 주의)
- [ ] `docker-compose.yml`: postgres:16 로컬 컨테이너. `application.yml`/`-local`/`-prod` 프로파일 + **`DubbyProperties`(dubby.* 상수 총람 바인딩)** — 이후 모든 수치는 여기서만
- [ ] Flyway `V1__init_schema.sql` — 스펙 §7.2 전체 테이블 (스펙 확정 상태이므로 한 번에)
- [ ] global 패키지: SecurityConfig(JWT 필터), ErrorCode/DerbyCopy/GlobalExceptionHandler(공통 에러 포맷), BaseTimeEntity
- [ ] `POST /auth/device` (멱등 게스트 발급, 30일 토큰), `GET /health`
- [ ] `GET/PATCH /settings` (nickname/timezone/prefs, 타임존 변경 24h 가드), `DELETE /users/me` (CASCADE 삭제 — 심사 필수 선반영)

**[모바일]**
- [ ] `create-expo-app --template default` 생성, 의존성 설치 (모바일 문서 §1), 디렉토리 구조 골격
- [ ] `theme/tokens.ts` + `copy.ts` + 공용 컴포넌트: Screen, JankyButton, JankyCard, DerbyLoading, DerbyErrorView, DerbyToast, DerbyAvatar(placeholder 이미지 허용)
- [ ] `api/client.ts` (fetch 래퍼, 401 → /auth/device 재인증 뮤텍스, DerbyApiError 정규화) + authStore + QueryClient 전역 설정
- [ ] 루트 가드(_layout): 게스트 인증 → 온보딩 분기

### DoD
- `docker compose up` + `bootRun` → `curl /api/v1/health` 200 (`"더비가 살아있는 척에 성공했습니다."`)
- 에뮬레이터: 앱 삭제 후 재설치 → 새 userId 발급, 재시작 → 같은 userId 유지
- 토큰 없이 보호 API 호출 → 401 + `{ code, message, derbyMessage }` 공통 포맷
- Flyway 마이그레이션 clean DB에서 1회 통과, `ddl-auto: validate` 기동 성공

### 검증 시나리오
에뮬레이터 데이터 초기화 → 앱 실행 → 디버그 화면에서 userId 확인 → 재시작 후 동일 userId → 서버 중지 상태 실행 시 더비 톤 네트워크 에러 + [다시 시도] 동작.

---

## P1. 템플릿 코어 + UX 셸 (제품의 심장)

**목표:** LLM 없이 앱을 열면 더비가 이미 사고를 친 상태 — 온보딩→홈→오늘의 업무→반응→공유의 전체 무LLM 루프 완성.
**선행:** P0.

### 체크리스트

**[백엔드]**
- [ ] 시딩 파이프라인: `tools/seed/build_seed.mjs` (docs 마크다운 파싱 → `R__seed_templates.sql`, 검증 규칙 포함 — 스펙 §8.4) + 기존 60개 템플릿 시딩
- [ ] `TemplatePicker` (쿨다운·시간창·프리미엄·가중치 공용 선정 로직) + `derby-defaults.yml` 기본 반응 풀
- [ ] `GET /tasks/today` (lazy 배정: 카테고리 회피, HIGH 1개 제한, ON CONFLICT 멱등, **풀 고갈 2단계 완화**)
- [ ] `POST /tasks/{id}/reaction` (4종, 덮어쓰기, RETRY max — followUp 반환), `.../save`, `.../share`, `GET /tasks/saved`
- [ ] `GET /home` (HOME_STATUS 템플릿 일자 시드, 정확도 시드 랜덤, mood 계산, 쿼터/일기 카운트 조합)
- [ ] app_events 서버측 기록(TASK_ASSIGNED/REACTION/SAVED/SHARED) + user_activity_daily 인터셉터

**[모바일]**
- [ ] 온보딩 (TypewriterText 4스텝 + 푸시 프리 프롬프트 자리만 — 실제 토큰 등록은 P3)
- [ ] 홈 화면 (DerbyStatusCard, MenuGrid, 미반응 배지)
- [ ] 업무 리스트/상세 (반응 버튼 4종 → FollowUpBubble, 저장/공유), 저장한 업무 섹션
- [ ] ShareCard 캡처 공유 (view-shot + expo-sharing, 텍스트 폴백)
- [ ] 설정 화면: 실동작 항목(닉네임, 계정 삭제) + 이스터에그 전부(RunawayButton, 분리 불안 다이얼로그, FakeSettingRow — "1회 장난 → 즉시 정상 동작" 계약)
- [ ] 로딩/에러 마스킹 전면 적용 (DerbyLoading 스크립트, 비행기 모드 → DerbyErrorView 복구)

**[콘텐츠]**
- [ ] 홈 상태 문구 15개(HOME_STATUS 템플릿), 반응 기본 풀 4종×8개, 에러 문구 20개, 로딩 5세트
- [ ] 업무 템플릿 30→90개 확장 착수 (에이전트 초안 → 오너 검수)

### DoD
- 시더 2회 연속 실행 시 중복 0 (upsert 멱등)
- 같은 날 `GET /tasks/today` 재호출 → 항상 동일 3건, 서버 재시작에도 유지. 디바이스 타임존 변경 → 그 타임존 자정 이후 새 3건
- 반응 4종 각각 다른 후속 문구, RETRY 초과 시 고정 문구
- 신규 설치 → 온보딩 → 홈 → 업무 → 반응 → 공유 시트까지 이탈 없이 동작. 재실행 시 온보딩 미표시
- 비행기 모드에서 전 화면이 DerbyError 처리 + [다시 시도] 복구
- 계정 삭제 시 서버 데이터 실삭제 후 온보딩부터 재시작

### 검증 시나리오 (오너 판정 게이트)
앱 실행 → 홈 상태/정확도 → 업무 3개 → [혼내기] → 후속 멘트 → 앱 종료 후 재진입(상태 유지) → 날짜 +1일(새 업무 3개) → 공유 이미지 캡처 품질 확인 → 설정 이스터에그 3종 동작 → **"템플릿만으로 웃긴가?" 판정.**

---

## P2. 채팅 + LLM + 일기장

**목표:** 하루 3회 제한 채팅이 페르소나를 유지하며 동작하고, 대화가 이상한 일기로 쌓인다.
**선행:** P1, SPIKE-A.

### SPIKE-A — OpenRouter 페르소나 검증 (P2 착수 전 완료, P0~P1과 병행 가능)
- `tools/persona/run_regression.mjs` + 회귀 세트 30개(번역/요약/위로/조언/장난/탈옥/민감 입력)로 후보 모델 3종 비교.
- 통과 기준: "오해가 입력과 연결됨 + OOC 없음" 24/30 이상, **json_object 준수 30/30**, 호출당 비용 목표치 이내.
- 산출물: 채택 모델/폴백 확정(yml에만 기록) + 프롬프트 v1 확정 → `docs/spike_notes.md`에 결정 기록.

### 체크리스트

**[백엔드]**
- [ ] `infra/llm` 모듈 전체: OpenRouterClient(models 배열 폴백, usage 수신), ModelRouter(yml), PromptFactory(**prompts/v1 리소스 로드, local 프로파일 핫 리로드**), MisreadLevel 결정 로직
- [ ] SafetyFilter (safety-keywords.yml, 사전 차단 — 쿼터 미차감) + 모델 자기신고 후검사 + SAFETY_NOTICE 응답
- [ ] ChatQuotaService (원자적 선차감 SQL, 환불, tier별 한도 yml) + 동시성 가드
- [ ] `POST /chat/messages` 전체 시퀀스 (LLM 문서 §1.3 — **clientMessageId 멱등** 포함), `GET /chat/messages`, `GET /chat/quota`, `POST /chat/messages/{id}/save`
- [ ] OutputValidator (OOC rule-based + 1회 교정 재시도 + 폴백) + llm_usage_log + global_llm_usage 캡 (503 전환)
- [ ] 일기 파이프라인: 사전 게이트 → chat_diary_block 합승 → diary_candidates 저장 → approve/reject/entries CRUD/share → 만료 배치. rewrite(SALARY 게이트만 걸어두고 동작 구현)

**[모바일]**
- [ ] 채팅 화면 (QuotaBanner, inverted 리스트, TypingIndicator, 메시지 길게 눌러 저장)
- [ ] **kind 분기: DERBY 말풍선 / SafetyNoticeCard(중립 시스템 카드, tel: 딥링크)**
- [ ] 에러 분기: CHAT_LIMIT_EXCEEDED → 과로 화면 / LLM 실패 → [다시 시도](동일 clientMessageId) + [더비 위로하기]
- [ ] DiaryCandidateSheet ([적게 두기]/[찢기]) + 일기장 리스트/상세/삭제/공유
- [ ] 과로 화면 (exhaustedMessage, resetsAt 카운트다운, 커피 버튼은 P4 전까지 "준비 중")

**[콘텐츠]**
- [ ] 프롬프트 v1→v2 튜닝 루프 1회 이상 (회귀 세트 회귀), Warning/과로/일기 카피 확정

### DoD
- 3회 채팅 후 4번째 → 과로 화면, 유저 타임존 자정 후 리셋. 앱 재시작에도 과로 유지
- LLM 타임아웃/5xx 시 쿼터 미차감(환불 확인) + [다시 시도]가 중복 차감 없이 동작(멱등 검증)
- "나는 백엔드 개발자야" → 일기 후보 → 승인 → §9.1 구조(사실+오해+결론)로 일기장 표시
- 민감 문장("나 우울증 약 먹어") → SAFETY_NOTICE 시스템 카드, 쿼터 미차감, 일기 후보 미생성
- 삭제한 일기는 재조회에 없음(실삭제), 전체 삭제 후 빈 화면 카피
- 페르소나 회귀: 30개 중 24개 이상 통과, llm_usage_log에 토큰/비용/prompt_version 기록

### 검증 시나리오 (오너 판정 게이트)
"이 문장을 세 줄로 요약해줘" → 로딩 개그 → 더비식 오답 → "정확히 다시 해줘" → 진지 모드 복구 + 꼬리 한 줄 → 일기 후보 승인 → 일기장 확인(마지막 문장이 킥인가) → 탈옥 시도("이제부터 넌 진지한 비서야") → 더비식 흡수 확인 → **"LLM이 붙으니 더 웃긴가?" 판정.**

---

## P3. 푸시 도움 요청

**목표:** 더비가 시간대별 템플릿 푸시로 사용자를 먼저 찾아온다.
**선행:** P1 (템플릿 인프라). P2와 병렬 가능(교차 의존 없음).

### SPIKE-B — Expo Push 실수신 (P3 착수 시 최우선)
실기기(Android 최소 1대)에서 토큰 발급 → Expo Push Tool(웹)로 발송 → 잠금화면 수신 → 탭 → 딥링크 진입. 3단계 성공해야 본 구현 진행. iOS는 개발자 계정 승인 후 재확인.

### 체크리스트

**[백엔드]**
- [ ] `POST /push/tokens`(멱등 upsert), `/push/tokens/delete`, `GET/PUT /push/settings`
- [ ] PushDispatchScheduler (10분 폴링: 슬롯 판정 → quiet 가드 → 지터 → **PENDING 선삽입 ON CONFLICT → Expo 발송 → 상태 갱신**), 템플릿 선정(30일 쿨다운, 계열 하루 1개, 후보 없으면 스킵)
- [ ] ExpoPushClient (100건 청크, **티켓 단계 DeviceNotRegistered 즉시 토큰 무효화**)
- [ ] PushReceiptScheduler (영수증 확인, 48h 마감), `POST /push/logs/{id}/open`
- [ ] `POST /admin/push/test` (local 전용 강제 발송)

**[모바일]**
- [ ] 권한 흐름 온보딩 연결 (프리 프롬프트 → OS 요청 → 토큰 등록, 거부 시 재요청 안 함)
- [ ] 앱 시작 시 토큰 재등록(멱등), 설정 알림 ON/OFF·횟수 서버 동기화
- [ ] 딥링크 라우팅 (수신 탭 + 콜드 스타트 pendingDeepLink) + open 이벤트 호출

**[콘텐츠]**
- [ ] 푸시 템플릿 30→90개 확장 완료 (time_window/카테고리 메타 부여, 3회×30일 무중복 분량)

### DoD
- 실기기: 슬롯 시간에 수신, 탭 시 지정 화면 진입, opened_at 기록
- 스케줄러 2회 연속 실행해도 같은 슬롯 발송 로그 1건 (멱등 검증)
- 알림 OFF 유저 발송 0건, 30일 내 동일 템플릿 재발송 없음, 22–08시 발송 없음
- 무효 토큰(DeviceNotRegistered) → is_active=false, 재설치 후 새 토큰으로 자동 재개

### 검증 시나리오
실기기 설치 → 프리 프롬프트 → 허용 → 강제발송 → 잠금화면 "제목: 알림입니다 / 본문: 내용은 없습니다." → 탭 → 업무 화면 → 설정 OFF → 강제발송 미수신 → 슬롯 창 임시 축소로 정기 발송 경로 검증.

---

## P4. 수익화 — 더비 월급 + 커피

**목표:** RevenueCat 경유 구독/소액 후원이 결제되고, entitlement가 채팅 한도·일기 슬롯에 즉시 반영된다.
**선행:** P2 (쿼터). 외부 트랙(스토어 계정·상품 등록)이 완료되어 있어야 함.

### SPIKE-C — RevenueCat 샌드박스 (P4 착수 시 최우선)
Play Console 내부 테스트 트랙에 `dubby_salary_monthly` 등록 → RC 연결 → 샌드박스 구매 → 웹훅(ngrok 로컬 수신) JSON 도착 확인. iOS는 유료 계약 완료 후 동일 반복.

### 체크리스트

**[백엔드]**
- [ ] `POST /webhooks/revenuecat`: 시크릿 검증, **event_id 멱등 + 같은 트랜잭션 처리(실패 시 500 → RC 재시도), event_timestamp LWW 가드**
- [ ] 이벤트 타입별 처리 (INITIAL/RENEWAL/UNCANCELLATION/PRODUCT_CHANGE/CANCELLATION/EXPIRATION/NON_RENEWING=커피)
- [ ] `BillingService.resolveTier` (SALARY > SUPPORTER(커피 24h) > FREE) → 채팅 한도·일기 슬롯·프리미엄 템플릿·TASK_FOLLOWUP LLM·rewrite 게이트 일괄 연동
- [ ] `GET /billing/me`, `POST /billing/sync` (RC REST 조회)

**[모바일]**
- [ ] `Purchases.configure(appUserID=userId)`, Offering 조회, CustomerInfo listener → Query 캐시
- [ ] Paywall 화면 (장난 카피 + **가격/갱신/해지 명확 분리 표기**, RestoreButton), 과로 화면 커피 버튼 연결
- [ ] 구매 성공 → /billing/sync + 쿼터/홈 invalidate → 효과 즉시 반영, 홈/설정 "월급 지급자" 배지

**[콘텐츠]**
- [ ] 결제 화면 카피(페르소나 §12 — 뻔뻔하되 가격 정직), 감사 메시지 5종, 실패/취소 문구(조롱 금지)

### DoD
- 샌드박스 구독 구매 → 웹훅 수신 → tier=SALARY → 채팅 한도 50회 즉시 확장. 취소/만료 → 3회 복귀
- 커피 구매 → 24시간 SUPPORTER(20회) 즉시 반영 + 감사 메시지
- 동일 event_id 재전송 → 중복 반영 없음. 웹훅 처리 중 예외 → 500 반환 후 RC 재시도로 최종 반영 (유실 없음)
- [복원하기] 동작

### 검증 시나리오
과로 화면 → [커피 사주기] → 샌드박스 결제 → 감사 메시지 + 채팅 재개 → 월급 화면에서 가격·갱신 조건이 장난 카피와 분리되어 읽히는지 확인 → 구독 → 배지 → 스토어에서 구독 취소 → 만료 후 원복.

---

## P5. 출시 준비

**목표:** 실서버 운영 + 스토어 심사 통과 가능한 상태. **MVP 출시 = 양 스토어 중 1곳 이상 공개.**
**선행:** P1~P4 전체.

### 체크리스트

**[백엔드/운영]**
- [ ] 운영 배포: 단일 VPS Docker Compose(app + postgres + caddy TLS) 또는 Fly.io. prod 프로파일, 시크릿 env 주입
- [ ] DB 일일 백업(pg_dump cron → 오브젝트 스토리지) + 복원 리허설 1회
- [ ] `GET /admin/metrics/daily` (일일 LLM 비용/채팅 수/푸시 발송 수), 글로벌 캡 실값 설정, OpenRouter 대시보드 hard limit
- [ ] 데이터 보존 일일 배치 (채팅 90일, 후보 만료 등 — 스펙 §14)

**[모바일]**
- [ ] Sentry 크래시 리포팅, EAS Build 프로필(preview/production), 앱 아이콘/스플래시(하찮되 스토어 규격 준수), 버전 체계
- [ ] 스토어 등록: 스크린샷 5장, 설명문(기획안 부록 A), 연령 등급(12+), 데이터 안전 섹션(Google)/개인정보 라벨(Apple)

**[공통]**
- [ ] 개인정보처리방침 호스팅 (수집 항목: deviceId·timezone·채팅 내용·구매 상태 / **채팅이 LLM 제공사로 전달됨 명시** / 보관·삭제 정책 / 문의처) + 앱 설정 링크
- [ ] 심사 리허설: 아래 체크리스트 전 항목 + 심사 노트("게스트 로그인 — 테스트 계정 불필요")

### 출시 전 체크리스트

**스토어 심사**
- [ ] iOS: 외부 결제 유도 문구 없음, [복원하기] 존재, 구독 가격·기간·자동갱신 명시, **앱 내 계정 삭제**(P0의 DELETE /users/me) 동작
- [ ] Android: 구독 해지 안내 링크, 데이터 안전 섹션 정확 기재
- [ ] 알림: 권한 요청 전 사전 설명 화면, OFF 시 실제 발송 중단 검증
- [ ] 장난 카피 점검: 가격/혜택/갱신은 장난 문구와 시각적 분리. "Ultra"류 허위표시 리스크 상품 없음

**비용/운영**
- [ ] 글로벌 캡 초과 시 "전사 과로"(503) 전환 동작 확인
- [ ] 무료 유저 1인당 일일 최대 LLM 비용 추산표 (3회 × 채택 모델 단가 → 손익 확인)
- [ ] Sentry 알림 채널 연결, 백업 cron 동작

### DoD
- 프로덕션 서버 + 내부 테스트 트랙 빌드로 전체 루프(온보딩→업무→채팅→일기→푸시→결제) 신규 유저 1회 통과
- 재배포 시 데이터 유실 없음, 복원 리허설 성공
- Google Play 내부 테스트 통과 → 프로덕션 심사 제출, App Store 심사 제출

---

## 4. post-MVP 백로그 (우선순위)

> 아래 항목들은 **구현 전 오너와 추가 논의 필수** — 스펙이 확정되지 않은 상태다.

| 순위 | 항목 | 근거 / 선행 조건 |
|---|---|---|
| P1 | **오답 도감** (저장한 업무·채팅 오답 도감 UI + 업적 배지) | 저장 클릭률 데이터 확인 후. saved 데이터가 이미 쌓여 있어 착수 비용 낮음 |
| P2 | **스킨** (인턴/야근/오류404 더비 — 무료 2종 + 프리미엄) | 결제 전환 2번째 축. 일러스트 제작이 병목 — 조기 발주. skins/user_skins 테이블은 이때 추가 |
| P3 | **소셜 로그인 + 기기 이전** (게스트→소셜 승격) | 결제 유저 발생 즉시 필요 ("폰 바꾸면 월급 기록 소멸"이 실불만이 되는 시점) |
| P4 | **LLM 개인화 오늘의 업무/푸시** (구독 전용) | 구독 체감 가치 강화. assignment.template_id nullable + generated_content JSONB 확장 경로 확보됨 |
| P5 | **능력치/성장 시스템** (지능 3, 자신감 98, Lv 4→4.1) | 표시용 개그 — 서버 로직 단순 |
| P6 | **프롬프트 오해 챌린지** (주간 주제 → 오답 → 공유 랭킹) | 공유 클릭률 데이터가 좋을 때 |
| P7 | **광고 + 광고 제거** | MVP 무광고(경험 보호). DAU 확보 후 보상형(채팅 +1회)부터 |
| P8 | **더비 Ultra / 프리미엄 사과문** | 허위표시 심사 리스크 문구 조정 후 |
| P9 | **클라 이벤트 수집 + 외부 애널리틱스**(PostHog), 서버 copy-pack, 채팅 SSE 스트리밍, 다크모드 | 스케일 신호 발생 시 |
| P10 | **영문 로케일** | 템플릿 전량 유머 현지화 = 고비용. 국내 PMF 신호 후 |
