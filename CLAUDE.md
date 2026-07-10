# CLAUDE.md — 더비(Dubby) 프로젝트 작업 지침

더비는 "사용자를 돕고 싶지만 매번 사고 치는 하찮은 AI 비서" 컨셉의 코미디 캐릭터 모바일 앱이다.

## 필독 문서 (docs/)

| 문서 | 역할 |
|---|---|
| `derby_product_plan_v1.md` | 제품 기획 — 컨셉, MVP 범위, BM |
| `derby_persona_bible_v1.md` | 페르소나 — 말투, 오해 공식, 금지사항. **모든 사용자 노출 문구의 검수 기준** |
| `derby_system_spec_v1.md` | **API 계약·DB 스키마·에러 코드의 SSOT.** 백엔드 구현은 반드시 이 문서 준수 |
| `derby_llm_pipeline_v1.md` | LLM 파이프라인 — 프롬프트, 안전 처리, 비용 통제 |
| `derby_mobile_architecture_v1.md` | 모바일 구조 — 화면, 상태 관리, 디자인 시스템 |
| `derby_roadmap_v1.md` | 개발 순서 (P0~P5) — 페이즈별 체크리스트·DoD |
| `handover.md` | **현재 진행 상황 + 인수인계.** 세션 시작 시 반드시 읽고, 중요한 진척마다 갱신 |

## 작업 규칙

1. **에이전트 페이스**: 시간 견적 없이 의존성·DoD 기준으로만 진행. 크리티컬 이슈(오너 승인 필요)가 아니면 멈추지 않고 계속 작업한다.
2. **상수는 yml**: 튜닝 가능성 있는 모든 수치는 `dubby.*` (`DubbyProperties`)에만. 하드코딩 금지.
3. **프롬프트는 리소스 파일**: `dubbyServer/src/main/resources/prompts/{version}/`. 코드에 문자열 금지.
4. **스키마 변경은 오직 Flyway**: `ddl-auto: validate`. 시딩은 `tools/seed/build_seed.mjs` → `R__seed_templates.sql`.
5. **에러 응답**: `{ code, message, derbyMessage }`. 에러 코드 문자열은 클라 분기 계약 — 변경 금지.
6. **장난 금지 구역**: 결제·인증·계정 삭제·안전 관련 문구는 명확성 우선.
7. **OOC는 서비스의 죽음**: 더비가 진지해지거나 똑똑해지는 문구·응답 금지. 고위험 입력만 시스템 카드로 예외.
8. **문구 검수 기준**: "이건 진짜로 불편한가, 아니면 더비답게 어이없는가?" — 불편하면 제거.

## 빌드/실행

```bash
# 백엔드 (JAVA_HOME=JDK21 설정됨, java가 PATH에 없어도 gradlew는 동작)
cd dubbyServer && ./gradlew build          # Windows: .\gradlew.bat build
docker compose up -d                        # 로컬 PostgreSQL 16 (dubbyServer/docker-compose.yml)
./gradlew bootRun --args='--spring.profiles.active=local'

# 모바일
cd dubbyMobile && npm install && npx expo start
```

## 커밋 규칙

- 브랜치: `main` 직접 커밋 (1인 + 에이전트 개발).
- 메시지: `[P{phase}] <요약>` (예: `[P0] 게스트 인증 + 공통 에러 처리`).
- 의미 있는 단위마다 커밋·푸시하고, 페이즈 경계 또는 큰 진척마다 `docs/handover.md` 갱신을 같은 커밋에 포함.
