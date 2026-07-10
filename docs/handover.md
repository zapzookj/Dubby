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

### 진행 중 (P1)
- [ ] 시딩 파이프라인(tools/seed/build_seed.mjs → R__seed_templates.sql) + 60개 템플릿
- [ ] TemplatePicker + GET /tasks/today(lazy 배정) + reaction/save/share + GET /home
- [ ] 모바일: 홈 실장(GET /home), 업무 리스트/상세, 설정 화면+이스터에그

### 다음 작업
- P1 완료 후 → P2 (채팅+LLM+일기장, SPIKE-A 선행)

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
