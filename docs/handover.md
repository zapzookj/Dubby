# 더비 개발 진행 상황 & 인수인계

> 다른 세션/에이전트가 작업을 이어받을 때 이 파일부터 읽는다.
> 갱신 규칙: 페이즈 경계 또는 의미 있는 진척마다 갱신 (너무 잘게 갱신하지 않음).

---

## 현재 상태 (2026-07-10)

**페이즈: P0 파운데이션 — 진행 중**

### 완료
- [x] 설계/스펙 확정 및 문서화 (docs/ 4종: system_spec, llm_pipeline, mobile_architecture, roadmap — 오너 승인 완료)
- [x] 모노레포 구성: 루트 git init(main), dubbyServer/.git 제거(커밋 없었음), .gitignore, CLAUDE.md
- [x] 원격: https://github.com/zapzookj/Dubby.git

### 진행 중
- [ ] P0 백엔드: build.gradle 의존성, DubbyProperties, Flyway V1, 인증(/auth/device), 공통 에러, settings, 계정 삭제
- [ ] P0 모바일: create-expo-app 스캐폴드, 테마/공용 컴포넌트, api client

### 다음 작업
- P0 잔여 → P1 (시딩 파이프라인 + 오늘의 업무 + 홈 + UX 셸)

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

- (없음)
