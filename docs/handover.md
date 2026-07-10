# 더비 개발 진행 상황 & 인수인계

> 다른 세션/에이전트가 작업을 이어받을 때 이 파일부터 읽는다.
> 갱신 규칙: 페이즈 경계 또는 의미 있는 진척마다 갱신 (너무 잘게 갱신하지 않음).

---

## 현재 상태 (2026-07-10)

**페이즈: P0 완료 → P1 착수**

### 완료 (P0)
- [x] 설계/스펙 확정 및 문서화 (docs/ 4종 — 오너 승인 완료). 원격: https://github.com/zapzookj/Dubby.git
- [x] 백엔드 파운데이션: Boot 4.1 의존성(**spring-boot-starter-flyway 필수** — flyway-core만으론 자동구성 안 됨), yml 3종 + DubbyProperties(상수 총람), Flyway V1(전체 스키마), SecurityConfig(JWT)+공통 에러(ErrorCode/DerbyCopy), POST /auth/device, GET·PATCH /settings, DELETE /users/me
- [x] 백엔드 DoD 실검증 통과: 멱등 등록/재로그인, 401 공통 포맷, 닉네임 12자·타임존 24h 가드, 계정 삭제 후 신규 발급, Flyway clean-DB 통과
- [x] 모바일 파운데이션: Expo SDK 57(default 템플릿, **라우트가 src/app/ — 문서의 루트 app/과 다름, 템플릿 컨벤션 따름**), 테마 토큰/copy.ts, api client(fetch+401 재인증 뮤텍스), 스토어 3종, 공용 컴포넌트(Screen/JankyButton/JankyCard/DerbyAvatar/DerbyLoading/DerbyErrorView/DerbyToast), 온보딩(타이핑 4스텝), 홈 임시판(헬스체크 표시), tsc + expo-doctor 20/20 통과

### 완료 (P1)
- [x] 시딩 파이프라인: tools/seed/build_seed.mjs → R__seed_templates.sql (75개: 업무30/푸시30/홈상태15). **docs 마크다운이 콘텐츠 SSOT** — 템플릿 수정 절차: docs 편집 → `node tools/seed/build_seed.mjs` → 커밋 (생성 SQL 직접 수정 금지). 홈 상태 문구는 신규 문서 docs/derby_home_status_v1.md
- [x] 백엔드: TemplatePicker(쿨다운/가중셔플/카테고리회피/2단계 완화), /tasks/today lazy 배정(ON CONFLICT 멱등), reaction(4종 덮어쓰기, RETRY max=2)/save/share/saved, /home(HOME_STATUS 시드고정+정확도+mood), derby-defaults.yml(버튼 문구 풀), app_events+user_activity_daily
- [x] 백엔드 DoD 실검증 11개 시나리오 통과
- [x] 모바일: 홈 실장(상태카드+메뉴그리드+미반응 배지), 업무 리스트/상세(반응버튼→후속 말풍선, RETRY 소진은 개그로 표시), 도감 저장/공유(ViewShot 이미지 캡처+텍스트 폴백), 저장 목록(무한스크롤), 설정(닉네임/이스터에그 3종/RunawayButton/계정삭제 이중확인). 채팅/일기/월급 메뉴는 "준비 중" 토스트. tsc 통과

### 완료 (P2) — 채팅 + LLM + 일기장
- [x] infra/llm: LlmClient 인터페이스 + **OpenRouterClient(운영) / MockLlmClient(로컬 기본, `dubby.llm.mock`)**. 프롬프트는 `resources/prompts/v1/*.md` (local 프로파일 핫리로드 — 재시작 없이 튜닝)
- [x] 채팅: 원자적 쿼터 선차감/환불, clientMessageId 멱등, 동시성 가드, 오해강도(Lv1/Lv2, 연속 Lv2 방지), 안전 2중(사전 정규식 safety-keywords.yml + 모델 자기신고), OOC 검증+1회 교정, 429 확장 필드(resetsAt/paywallHint)
- [x] 일기: 채팅 합승 후보(게이트: 자기서술 패턴/민감어/일일상한/빈도) → approve/reject → 실삭제 CRUD/공유. rewrite는 SALARY 게이트(403, LLM 연결은 P4)
- [x] 모바일: 채팅 화면(세션 아이템 방식, kind 분기, SafetyNoticeCard 중립 카드, 일기 후보 카드, 에러 시 동일 cmid 재시도), 과로 화면, 일기장 리스트/상세/삭제/공유, 홈 메뉴 연결
- [x] DoD: mock LLM으로 15개 백엔드 시나리오 통과, tsc 통과
- [x] tools/persona/: SPIKE-A 회귀 실행기(run_regression.mjs) + 세트 30케이스 준비 완료

### ⚠ SPIKE-A 미완 (오너 액션 필요)
실모델 페르소나 검증은 **OPENROUTER_API_KEY가 필요**해 보류 중. 절차:
`OPENROUTER_API_KEY=sk-... node tools/persona/run_regression.mjs` → 결과 파일(tools/persona/results/) 오너 검수(기준: 오해+OOC없음 24/30, json 30/30) → 채택 모델을 application.yml `dubby.llm.routing`에 반영 → 서버는 `LLM_MOCK=false` + 키로 실모델 전환.
**P4(수익화) 전까지만 완료되면 출시 크리티컬 패스에 영향 없음.**

### 완료 (P3 — 푸시)
- [x] 백엔드: POST /push/tokens(멱등 upsert)+tokens/delete, GET·PUT /push/settings(OFF는 발송 완전 제외), PushDispatchScheduler(10분 폴링: 슬롯/quiet 22-08 하드가드/지터 → 템플릿 선정(30일 쿨다운·계열 하루 1개·{nickname} 치환) → **PENDING 선삽입 ON CONFLICT** → Expo 발송 → 티켓 반영), ExpoPushClient(100/1000건 청크, 티켓·영수증 양단 DeviceNotRegistered 즉시 무효화), PushReceiptScheduler(15분, 48h 마감), POST /push/logs/{id}/open, POST /admin/push/test(local 전용)
- [x] 모바일: notifications/setup(프리 프롬프트→OS 권한→토큰 등록, 앱 시작 시 silent 재등록, **EAS projectId 없으면 우아하게 스킵**), 딥링크 라우팅(실행 중 리스너+콜드스타트 pendingDeepLink→(main) 가드 후 소비), 온보딩 마지막 스텝 프리 프롬프트, 설정 알림 토글(분리불안 다이얼로그 1회 후 확실 반영)+횟수 1~3 선택
- [x] DoD: 엔드포인트 10개 시나리오 통과(멱등 등록/범위 가드/OFF/오픈 추적), Expo API 실호출 확인(가짜 토큰 → DeviceNotRegistered 티켓 수신), 스케줄러 기동 무에러, tsc 통과

### ⚠ SPIKE-B 미완 (오너 액션 필요)
실기기 푸시 수신 검증 대기: ① EAS 프로젝트 생성(`eas init` → app.json에 projectId) ② 실기기(Android 우선) ③ 서버 `POST /api/v1/admin/push/test`로 즉시 발송 → 잠금화면 수신 → 탭 → 딥링크 진입 확인.

### 다음 작업 (P4 — 수익화)
- [ ] SPIKE-C: RevenueCat 샌드박스 (오너: Play Console 내부 트랙 + RC 프로젝트 + 상품 등록 `dubby_salary_monthly`/`dubby_coffee`, entitlement `salary`)
- [ ] 백엔드: RevenueCat 웹훅(event_id 멱등+같은 트랜잭션, LWW 가드), BillingService.resolveTier(SALARY>SUPPORTER>FREE) → 채팅 한도/일기 슬롯/rewrite/TASK_FOLLOWUP LLM 연동(현재 TODO(P4) 주석 4곳: TaskService.assign, ChatService 2곳, DiaryService.rewrite, HomeService), GET /billing/me, POST /billing/sync
- [ ] 모바일: react-native-purchases 설치(**이후 Expo Go 불가 — dev build 필요**), Paywall(/salary), 과로 화면 커피 연결
- 이후 P5(출시: 배포/모니터링/스토어)

---

## 환경 특이사항 (Windows 개발 머신)

- **JDK**: PATH에 java 없음. `JAVA_HOME=C:\Program Files\Java\jdk-21.0.1.12-hotspot` 설정돼 있어 gradlew는 동작. **build.gradle toolchain은 21로 설정** (원래 17이었으나 로컬 JDK에 맞춤 — Boot 4.1은 17+ 지원).
- **Docker**: 25.0.3 실행 중 — 로컬 PostgreSQL은 `dubbyServer/docker-compose.yml`.
- **gh CLI 없음**: git push는 Windows 자격증명 관리자의 GitHub 계정(zapzookj) 사용.
- **Node 24 / npm 11** 사용 가능.

## 승인된 핵심 결정 (요약 — 상세는 스펙 문서)

- 익명 디바이스 게스트 + 30일 액세스 토큰 단일 (refresh 없음). 401 → /auth/device 재호출.
- users PK = UUID (RevenueCat app_user_id 겸용). jjwt + **jjwt-gson** (Boot 4 Jackson 3 혼재 회피).
- 튜닝 상수 전부 `dubby.*` yml (`DubbyProperties`). DB 설정 테이블 금지.
- 반응 enum PRAISE/SCOLD/RETRY/IGNORE, 채팅 에러 `CHAT_LIMIT_EXCEEDED`, 응답 구분 `kind: DERBY|SAFETY_NOTICE`.
- 일기 실삭제, 후보 TTL 72h. 푸시 quiet 22–08 고정 가드, 슬롯당 1건 UNIQUE.
- entitlement `salary`, tier FREE/SUPPORTER/SALARY, 커피 24h 효과.
- MVP 제외(구현 전 오너 논의 필수): 스킨, 도감, 능력치, 소셜 로그인, 클라 이벤트 수집, Ultra, 다크모드, SSE.

## 오너 작업 방식 (준수)

- 크리티컬 이슈(승인 필요) 외에는 멈추지 않고 계속 작업. 태스크 완료 보고 불필요.
- 시간 견적·과세분화 금지. DoD 기준으로만.
- 진행 상황은 이 파일에 주기적으로 기록.

## 미해결 / 주의

1. **Android 에뮬레이터 로컬 실행 불가 (환경 이슈, 코드 무관)**: AVD `Medium_Phone_API_36.0` 기동 시 "too many emulator instances" abort → `-port 5560 -feature -Netsim`으로 우회하면 부팅은 시작되나 adb에 디바이스가 영영 안 잡힘(QEMU 멈춤 추정). 소프트웨어 GL(opengl32sw) 로드 실패 경고도 있음. **머신 재부팅 또는 Android Studio에서 AVD 재생성 후 재시도 권장** (오너 액션). 앱 검증은 tsc/expo-doctor + 백엔드 API 실검증으로 대체 완료. 실기기+Expo Go로도 확인 가능(`npx expo start` 후 QR).
2. **더비 표정 PNG 8종 미수급**: DerbyAvatar는 이모지 폴백으로 구현됨. PNG 수급 시 `dubbyMobile/src/components/DerbyAvatar.tsx` 한 파일만 교체하면 됨 (컴포넌트 계약 유지).
3. **외부 트랙 미착수 (오너 액션 필요)**: Apple Developer / Google Play Console 계정, RevenueCat 프로젝트, EAS 프로젝트 생성. P4(수익화) 전까지 필요.
4. react-native-purchases는 P4에서 설치 예정 (설치 시 Expo Go 불가 → dev build 필요해지므로 미룸).
