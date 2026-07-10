-- ============================================================================
-- R__seed_templates.sql — tools/seed/build_seed.mjs 생성물. 직접 수정 금지.
-- 소스(SSOT): docs/derby_daily_tasks_top30_v2.md,
--             docs/derby_push_notifications_top30_v1.md,
--             docs/derby_home_status_v1.md
-- Flyway repeatable: 파일 체크섬 변경 시 자동 재실행 (upsert 멱등)
-- ============================================================================

INSERT INTO templates (code, type, category, time_window, intensity,
                       requires_user_name, is_premium, cooldown_days, locale, tags, content)
VALUES
('TASK-001', 'DAILY_TASK', 'ANALYSIS', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"사용자님의 생산성을 분석했습니다.","conclusion":"사용자는 생산성이 있습니다. 어디 있는지는 모르겠습니다.","note":"추적은 계속하지 않겠습니다. 어렵습니다.","shareText":"더비의 업무 보고: 사용자님의 생산성을 분석했습니다.\n결론: 사용자는 생산성이 있습니다. 어디 있는지는 모르겠습니다."}'::jsonb),
('TASK-002', 'DAILY_TASK', 'ANALYSIS', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"오늘의 운세를 계산했습니다.","conclusion":"조심하세요. 뭔가가 일어날 수도 있고 안 일어날 수도 있습니다.","note":"적중률은 상황에 따라 달라집니다. 모든 상황이 포함됩니다.","shareText":"더비의 업무 보고: 오늘의 운세를 계산했습니다.\n결론: 조심하세요. 뭔가가 일어날 수도 있고 안 일어날 수도 있습니다."}'::jsonb),
('TASK-003', 'DAILY_TASK', 'SELF_IMPROVE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"번역 기능을 개선했습니다.","conclusion":"이제 한국어를 한국어로 번역할 수 있습니다.","note":"예시 — “안녕하세요” → “안녕하세요”. 성공입니다.","shareText":"더비의 업무 보고: 번역 기능을 개선했습니다.\n결론: 이제 한국어를 한국어로 번역할 수 있습니다."}'::jsonb),
('TASK-004', 'DAILY_TASK', 'ANALYSIS', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"회의록 요약을 완료했습니다.","conclusion":"314줄의 회의록을 1058줄로 성공적으로 요약했습니다.","note":"핵심을 놓치지 않기 위해 전부 늘렸습니다.","shareText":"더비의 업무 보고: 회의록 요약을 완료했습니다.\n결론: 314줄의 회의록을 1058줄로 성공적으로 요약했습니다."}'::jsonb),
('TASK-005', 'DAILY_TASK', 'SCHEDULE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"사용자님의 일정을 정리했습니다.","conclusion":"모든 일정이 중요합니다.","note":"모든 일정을 ‘뭔가 해야 함’으로 분류했습니다.","shareText":"더비의 업무 보고: 사용자님의 일정을 정리했습니다.\n결론: 모든 일정이 중요합니다."}'::jsonb),
('TASK-006', 'DAILY_TASK', 'META_REPORT', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"업무 완료 보고","conclusion":"오늘도 아무도 시키지 않은 일을 해냈습니다.","note":"무슨 일이였는지는 까먹었습니다.","shareText":"더비의 업무 보고: 업무 완료 보고\n결론: 오늘도 아무도 시키지 않은 일을 해냈습니다."}'::jsonb),
('TASK-007', 'DAILY_TASK', 'USER_OBSERVE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"사용자의 오늘 상태를 진단했습니다.","conclusion":"사용자는 현재 존재합니다.","note":"존재 확인은 중요한 절차입니다.","shareText":"더비의 업무 보고: 사용자의 오늘 상태를 진단했습니다.\n결론: 사용자는 현재 존재합니다."}'::jsonb),
('TASK-008', 'DAILY_TASK', 'SELF_IMPROVE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"집중 모드를 준비했습니다.","conclusion":"집중할 준비를 하느라 집중하지 못했습니다.","note":"준비성은 있었습니다.","shareText":"더비의 업무 보고: 집중 모드를 준비했습니다.\n결론: 집중할 준비를 하느라 집중하지 못했습니다."}'::jsonb),
('TASK-009', 'DAILY_TASK', 'SCHEDULE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"할 일 목록을 최적화했습니다.","conclusion":"할 일을 모두 지우면 할 일이 없어집니다.","note":"매우 깨끗해졌습니다.","shareText":"더비의 업무 보고: 할 일 목록을 최적화했습니다.\n결론: 할 일을 모두 지우면 할 일이 없어집니다."}'::jsonb),
('TASK-010', 'DAILY_TASK', 'USER_OBSERVE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"사용자의 감정을 분석했습니다.","conclusion":"감정이 있는 것 같습니다.","note":"어떤 감정인지는 불명입니다.","shareText":"더비의 업무 보고: 사용자의 감정을 분석했습니다.\n결론: 감정이 있는 것 같습니다."}'::jsonb),
('TASK-011', 'DAILY_TASK', 'ANALYSIS', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"문서 정리를 완료했습니다.","conclusion":"문서를 정리하다가 문서와 정이 들었습니다.","note":null,"shareText":"더비의 업무 보고: 문서 정리를 완료했습니다.\n결론: 문서를 정리하다가 문서와 정이 들었습니다."}'::jsonb),
('TASK-012', 'DAILY_TASK', 'ANALYSIS', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"데이터 분석을 진행했습니다.","conclusion":"숫자들이 많았습니다. 그래서 진지한 표정을 지었습니다.","note":"실패는 성공의 어머니입니다.","shareText":"더비의 업무 보고: 데이터 분석을 진행했습니다.\n결론: 숫자들이 많았습니다. 그래서 진지한 표정을 지었습니다."}'::jsonb),
('TASK-013', 'DAILY_TASK', 'SCHEDULE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"사용자님의 휴식 계획을 수립했습니다.","conclusion":"쉬는 동안 쉬어야 합니다.","note":"이 결론에 도달하는 데 꽤 걸렸습니다.","shareText":"더비의 업무 보고: 사용자님의 휴식 계획을 수립했습니다.\n결론: 쉬는 동안 쉬어야 합니다."}'::jsonb),
('TASK-014', 'DAILY_TASK', 'SCHEDULE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"업무 우선순위를 정했습니다.","conclusion":"전부 1순위로 지정했습니다.","note":"모두 중요해 보였습니다.","shareText":"더비의 업무 보고: 업무 우선순위를 정했습니다.\n결론: 전부 1순위로 지정했습니다."}'::jsonb),
('TASK-015', 'DAILY_TASK', 'SCHEDULE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"사용자의 메모를 정리했습니다.","conclusion":"메모는 흩어져 있었고, 지금도 흩어져 있습니다.","note":"대신 제가 한번 쳐다봤습니다.","shareText":"더비의 업무 보고: 사용자의 메모를 정리했습니다.\n결론: 메모는 흩어져 있었고, 지금도 흩어져 있습니다."}'::jsonb),
('TASK-016', 'DAILY_TASK', 'SELF_IMPROVE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"더비가 스스로 업데이트를 시도했습니다.","conclusion":"바뀐 건 없지만 시간이 지났습니다.","note":"업데이트 느낌은 났습니다.","shareText":"더비의 업무 보고: 더비가 스스로 업데이트를 시도했습니다.\n결론: 바뀐 건 없지만 시간이 지났습니다."}'::jsonb),
('TASK-017', 'DAILY_TASK', 'SELF_IMPROVE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"자연어 처리 능력 대폭 개선","conclusion":"이제 사용자님의 말을 더 잘 이해합니다.","note":"예시 — 핵심만 말하라는 말은 핵심만 말하라는 뜻입니다.","shareText":"더비의 업무 보고: 자연어 처리 능력 대폭 개선\n결론: 이제 사용자님의 말을 더 잘 이해합니다."}'::jsonb),
('TASK-018', 'DAILY_TASK', 'ANALYSIS', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"오늘의 위험 요소를 탐지했습니다.","conclusion":"가장 위험한 요소는 제가 뭔가를 탐지했다는 사실입니다.","note":"그래도 탐지는 했습니다.","shareText":"더비의 업무 보고: 오늘의 위험 요소를 탐지했습니다.\n결론: 가장 위험한 요소는 제가 뭔가를 탐지했다는 사실입니다."}'::jsonb),
('TASK-019', 'DAILY_TASK', 'USER_OBSERVE', 'EVENING', 'LOW', false, false, 60, 'ko', '{}', '{"title":"사용자의 수면 패턴을 예측했습니다.","conclusion":"밤이 되면 잘 가능성이 있습니다.","note":"대담한 예측입니다.","shareText":"더비의 업무 보고: 사용자의 수면 패턴을 예측했습니다.\n결론: 밤이 되면 잘 가능성이 있습니다."}'::jsonb),
('TASK-020', 'DAILY_TASK', 'META_REPORT', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"더비의 사과문을 미리 작성했습니다.","conclusion":"아직 잘못하지 않았지만 대비는 끝났습니다.","note":"곧 쓸 일이 생길 것 같습니다.","shareText":"더비의 업무 보고: 더비의 사과문을 미리 작성했습니다.\n결론: 아직 잘못하지 않았지만 대비는 끝났습니다."}'::jsonb),
('TASK-021', 'DAILY_TASK', 'META_REPORT', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"사용자의 시간을 절약했습니다.","conclusion":"이 보고서를 읽는 시간만큼 시간이 줄었습니다.","note":"절약에는 실패했지만 측정에는 성공했습니다.","shareText":"더비의 업무 보고: 사용자의 시간을 절약했습니다.\n결론: 이 보고서를 읽는 시간만큼 시간이 줄었습니다."}'::jsonb),
('TASK-022', 'DAILY_TASK', 'META_REPORT', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"오늘의 질문을 준비했습니다.","conclusion":"질문은 “무엇을 질문해야 할까요?”입니다.","note":"질문 준비가 질문이 됐습니다.","shareText":"더비의 업무 보고: 오늘의 질문을 준비했습니다.\n결론: 질문은 “무엇을 질문해야 할까요?”입니다."}'::jsonb),
('TASK-023', 'DAILY_TASK', 'USER_OBSERVE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"사용자님의 기분을 좋게 만들 방법을 찾았습니다.","conclusion":"찾는 중입니다.","note":"찾고 있다는 사실이 위로가 되길 바랍니다.","shareText":"더비의 업무 보고: 사용자님의 기분을 좋게 만들 방법을 찾았습니다.\n결론: 찾는 중입니다."}'::jsonb),
('TASK-024', 'DAILY_TASK', 'SELF_IMPROVE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"더비가 버튼의 용도를 조사했습니다.","conclusion":"누르면 무슨 일이 일어납니다.","note":"안 누르면 아무 일도 안 일어납니다.","shareText":"더비의 업무 보고: 더비가 버튼의 용도를 조사했습니다.\n결론: 누르면 무슨 일이 일어납니다."}'::jsonb),
('TASK-025', 'DAILY_TASK', 'ANALYSIS', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"오늘의 성공 확률을 계산했습니다.","conclusion":"성공하거나 안 할 확률이 높습니다.","note":"계산은 완벽했습니다.","shareText":"더비의 업무 보고: 오늘의 성공 확률을 계산했습니다.\n결론: 성공하거나 안 할 확률이 높습니다."}'::jsonb),
('TASK-026', 'DAILY_TASK', 'USER_OBSERVE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"사용자의 말을 이해하려고 했습니다.","conclusion":"이해하려고 했다는 점은 이해했습니다.","note":"여기까지가 현재 실행 한계입니다.","shareText":"더비의 업무 보고: 사용자의 말을 이해하려고 했습니다.\n결론: 이해하려고 했다는 점은 이해했습니다."}'::jsonb),
('TASK-027', 'DAILY_TASK', 'SCHEDULE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"오늘의 목표를 설정했습니다.","conclusion":"목표는 목표를 정하는 것입니다.","note":"1단계에 오래 머무르는 중입니다.","shareText":"더비의 업무 보고: 오늘의 목표를 설정했습니다.\n결론: 목표는 목표를 정하는 것입니다."}'::jsonb),
('TASK-028', 'DAILY_TASK', 'META_REPORT', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"중요한 판단을 내렸습니다.","conclusion":"판단하지 않기로 판단했습니다.","note":"신중했습니다.","shareText":"더비의 업무 보고: 중요한 판단을 내렸습니다.\n결론: 판단하지 않기로 판단했습니다."}'::jsonb),
('TASK-029', 'DAILY_TASK', 'USER_OBSERVE', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"사용자님의 문장을 교정했습니다.","conclusion":"문장이 저보다 똑똑해서 건드리지 않았습니다.","note":"현명한 판단이었습니다.","shareText":"더비의 업무 보고: 사용자님의 문장을 교정했습니다.\n결론: 문장이 저보다 똑똑해서 건드리지 않았습니다."}'::jsonb),
('TASK-030', 'DAILY_TASK', 'META_REPORT', 'ANY', 'LOW', false, false, 60, 'ko', '{}', '{"title":"오늘의 업무 보고를 완료했습니다.","conclusion":"보고는 완료됐습니다. 업무는 보고와 별개입니다.","note":"이 부분이 중요합니다.","shareText":"더비의 업무 보고: 오늘의 업무 보고를 완료했습니다.\n결론: 보고는 완료됐습니다. 업무는 보고와 별개입니다."}'::jsonb),
('PUSH-001', 'PUSH', 'NO_CONTENT', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"알림입니다","body":"내용은 없습니다.","deeplink":"dubby://home"}'::jsonb),
('PUSH-002', 'PUSH', 'WORK_DONE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"업무 보고","body":"놀랍게도 제가 사용자님을 대신해 아무것도 안 했습니다. 실수 없이 완수했습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-003', 'PUSH', 'SELF_PRAISE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"생각 시작","body":"제가 생각을 시작했습니다. 대단하죠?","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-004', 'PUSH', 'SELF_PRAISE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"결론 도착","body":"엄청난 결론에 도달했습니다. 결론은 없습니다. 도달했다는 사실이 중요합니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-005', 'PUSH', 'NO_CONTENT', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"알림입니다","body":"제가 방금 알림을 보냈습니다. 이유는 지금부터 생각해보겠습니다.","deeplink":"dubby://home"}'::jsonb),
('PUSH-006', 'PUSH', 'SELF_PRAISE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"성장했습니다","body":"사용자님, 제가 오늘도 성장했습니다. 어제는 2+2를 5라고 했는데 오늘은 6이라고 했습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-007', 'PUSH', 'FAKE_URGENT', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"긴급 상황","body":"제가 긴급하다고 느꼈습니다. 사실 아무일도 없습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-008', 'PUSH', 'FAKE_URGENT', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"호출했습니다","body":"사용자님을 불렀습니다. 이제 할 말을 생각해보겠습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-009', 'PUSH', 'SEPARATION_ANXIETY', 'EVENING', 'LOW', false, false, 30, 'ko', '{}', '{"title":"대기 중","body":"제가 사용자님을 위해 기다리고 있습니다. 오시면 도망가겠습니다.","deeplink":"dubby://home"}'::jsonb),
('PUSH-010', 'PUSH', 'WORK_DONE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"보고드립니다","body":"제가 오늘도 아무것도 하지 않았습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-011', 'PUSH', 'HELP_REQUEST', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"도움 요청","body":"사용자님, 제가 지금 도움을 요청하는 척을 하고 있습니다. 도와주세요.","deeplink":"dubby://home"}'::jsonb),
('PUSH-012', 'PUSH', 'INCIDENT_REPORT', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"중요한 기억","body":"중요한 걸 까먹었습니다. 중요했다는 건 기억합니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-013', 'PUSH', 'INCIDENT_REPORT', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"학습 완료","body":"제가 방금 배웠습니다. 배운 내용은 아쉽게도 놓쳤습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-014', 'PUSH', 'FAKE_URGENT', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"긴급합니다","body":"제가 방금 중요한 결정을 미뤘습니다. 이유는 아직 못 정했습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-015', 'PUSH', 'WORK_DONE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"업무 완료","body":"업무가 없어서 빨랐습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-016', 'PUSH', 'WORK_DONE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"확인했습니다","body":"뭔지는 모르겠지만 확인했습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-017', 'PUSH', 'INCIDENT_REPORT', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"버튼 관련 보고","body":"제가 버튼을 눌렀습니다. 버튼도 놀랐을 겁니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-018', 'PUSH', 'SELF_PRAISE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"번역 기능 대폭 개선!","body":"이제 한국어를 한국어로 번역할 수 있습니다!","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-019', 'PUSH', 'WORK_DONE', 'MORNING', 'LOW', false, false, 30, 'ko', '{}', '{"title":"오늘의 계획","body":"계획을 세웠습니다. 계획은 없습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-020', 'PUSH', 'SELF_PRAISE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"계산 성공","body":"1 더하기 1은 2입니다. 오늘은 컨디션이 좋습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-021', 'PUSH', 'NO_CONTENT', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"시간 알림","body":"시간대 분석 결과 현재 시간은 지금입니다.","deeplink":"dubby://home"}'::jsonb),
('PUSH-022', 'PUSH', 'SELF_PRAISE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"참았습니다","body":"제가 방금 아무것도 안 눌렀습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-023', 'PUSH', 'INCIDENT_REPORT', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"좋은 생각","body":"좋은 생각이 났습니다. 생각보 떠다 빨리났습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-024', 'PUSH', 'SELF_PRAISE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"성공했습니다","body":"뭔가 성공했습니다. 뭔지 알면 알려드리겠습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-025', 'PUSH', 'HELP_REQUEST', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"사용자님 필요","body":"필요하다는 느낌이 듭니다.","deeplink":"dubby://home"}'::jsonb),
('PUSH-026', 'PUSH', 'SELF_PRAISE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"정답 보고","body":"제가 정답을 낸 것 같습니다. 문제는 없었습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-027', 'PUSH', 'WORK_DONE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"상태 보고","body":"자신감은 충분합니다. 이유는 부족합니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-028', 'PUSH', 'SELF_PRAISE', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"방금 이해했습니다","body":"뭔지는 아직 모릅니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-029', 'PUSH', 'WORK_DONE', 'MORNING', 'LOW', false, false, 30, 'ko', '{}', '{"title":"출근했습니다","body":"하는 일은 아직 출근하지 않았습니다.","deeplink":"dubby://tasks"}'::jsonb),
('PUSH-030', 'PUSH', 'NO_CONTENT', 'ANY', 'LOW', false, false, 30, 'ko', '{}', '{"title":"공지입니다","body":"공지할 일을 정하면 다시 공지하겠습니다.","deeplink":"dubby://home"}'::jsonb),
('HOME-001', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"근거 없는 자신감","currentWork":"사용자를 돕는 척하기"}'::jsonb),
('HOME-002', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"자신감 과다","currentWork":"중요한 버튼 찾는 중"}'::jsonb),
('HOME-003', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"대충 준비됨","currentWork":"준비된 척 유지하기"}'::jsonb),
('HOME-004', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"출근은 함","currentWork":"출근한 보람 찾는 중"}'::jsonb),
('HOME-005', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"오늘도 성장 중 (방향 미정)","currentWork":"성장 방향 정하기"}'::jsonb),
('HOME-006', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"집중력 충전 중","currentWork":"충전기 찾는 중"}'::jsonb),
('HOME-007', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"생각이 많음 (내용 없음)","currentWork":"생각 정리하는 척하기"}'::jsonb),
('HOME-008', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"의욕 초과 근무","currentWork":"의욕만 근무시키는 중"}'::jsonb),
('HOME-009', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"반성 기능 점검 중","currentWork":"반성할 일 미리 만들기"}'::jsonb),
('HOME-010', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"안정적으로 불안정","currentWork":"안정을 연기하는 중"}'::jsonb),
('HOME-011', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"오류 없음 (확인 안 함)","currentWork":"확인 안 하기"}'::jsonb),
('HOME-012', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"어제보다 나아짐 (어제 기준 미상)","currentWork":"어제 기억해내기"}'::jsonb),
('HOME-013', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"사용자님 대기 중","currentWork":"기다림 (주 업무)"}'::jsonb),
('HOME-014', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"눈치 업데이트 예정","currentWork":"업데이트 일정 미루기"}'::jsonb),
('HOME-015', 'HOME_STATUS', 'HOME_STATUS', 'ANY', 'LOW', false, false, 0, 'ko', '{}', '{"statusLine":"전원은 켜짐","currentWork":"켜져 있기"}'::jsonb)
ON CONFLICT (code) DO UPDATE SET
    type = EXCLUDED.type,
    category = EXCLUDED.category,
    time_window = EXCLUDED.time_window,
    intensity = EXCLUDED.intensity,
    requires_user_name = EXCLUDED.requires_user_name,
    is_premium = EXCLUDED.is_premium,
    cooldown_days = EXCLUDED.cooldown_days,
    content = EXCLUDED.content,
    content_version = templates.content_version + 1,
    updated_at = now()
WHERE (templates.content, templates.category, templates.time_window, templates.intensity,
       templates.is_premium, templates.cooldown_days)
      IS DISTINCT FROM
      (EXCLUDED.content, EXCLUDED.category, EXCLUDED.time_window, EXCLUDED.intensity,
       EXCLUDED.is_premium, EXCLUDED.cooldown_days);

-- docs에서 제거된 DAILY_TASK 템플릿 폐기 (물리 삭제 금지 — 이력/지표 보존), 복귀 시 재활성화
UPDATE templates SET status = 'RETIRED', updated_at = now()
WHERE type = 'DAILY_TASK' AND status = 'ACTIVE' AND code NOT IN ('TASK-001', 'TASK-002', 'TASK-003', 'TASK-004', 'TASK-005', 'TASK-006', 'TASK-007', 'TASK-008', 'TASK-009', 'TASK-010', 'TASK-011', 'TASK-012', 'TASK-013', 'TASK-014', 'TASK-015', 'TASK-016', 'TASK-017', 'TASK-018', 'TASK-019', 'TASK-020', 'TASK-021', 'TASK-022', 'TASK-023', 'TASK-024', 'TASK-025', 'TASK-026', 'TASK-027', 'TASK-028', 'TASK-029', 'TASK-030');
UPDATE templates SET status = 'ACTIVE', updated_at = now()
WHERE type = 'DAILY_TASK' AND status = 'RETIRED' AND code IN ('TASK-001', 'TASK-002', 'TASK-003', 'TASK-004', 'TASK-005', 'TASK-006', 'TASK-007', 'TASK-008', 'TASK-009', 'TASK-010', 'TASK-011', 'TASK-012', 'TASK-013', 'TASK-014', 'TASK-015', 'TASK-016', 'TASK-017', 'TASK-018', 'TASK-019', 'TASK-020', 'TASK-021', 'TASK-022', 'TASK-023', 'TASK-024', 'TASK-025', 'TASK-026', 'TASK-027', 'TASK-028', 'TASK-029', 'TASK-030');

-- docs에서 제거된 PUSH 템플릿 폐기 (물리 삭제 금지 — 이력/지표 보존), 복귀 시 재활성화
UPDATE templates SET status = 'RETIRED', updated_at = now()
WHERE type = 'PUSH' AND status = 'ACTIVE' AND code NOT IN ('PUSH-001', 'PUSH-002', 'PUSH-003', 'PUSH-004', 'PUSH-005', 'PUSH-006', 'PUSH-007', 'PUSH-008', 'PUSH-009', 'PUSH-010', 'PUSH-011', 'PUSH-012', 'PUSH-013', 'PUSH-014', 'PUSH-015', 'PUSH-016', 'PUSH-017', 'PUSH-018', 'PUSH-019', 'PUSH-020', 'PUSH-021', 'PUSH-022', 'PUSH-023', 'PUSH-024', 'PUSH-025', 'PUSH-026', 'PUSH-027', 'PUSH-028', 'PUSH-029', 'PUSH-030');
UPDATE templates SET status = 'ACTIVE', updated_at = now()
WHERE type = 'PUSH' AND status = 'RETIRED' AND code IN ('PUSH-001', 'PUSH-002', 'PUSH-003', 'PUSH-004', 'PUSH-005', 'PUSH-006', 'PUSH-007', 'PUSH-008', 'PUSH-009', 'PUSH-010', 'PUSH-011', 'PUSH-012', 'PUSH-013', 'PUSH-014', 'PUSH-015', 'PUSH-016', 'PUSH-017', 'PUSH-018', 'PUSH-019', 'PUSH-020', 'PUSH-021', 'PUSH-022', 'PUSH-023', 'PUSH-024', 'PUSH-025', 'PUSH-026', 'PUSH-027', 'PUSH-028', 'PUSH-029', 'PUSH-030');

-- docs에서 제거된 HOME_STATUS 템플릿 폐기 (물리 삭제 금지 — 이력/지표 보존), 복귀 시 재활성화
UPDATE templates SET status = 'RETIRED', updated_at = now()
WHERE type = 'HOME_STATUS' AND status = 'ACTIVE' AND code NOT IN ('HOME-001', 'HOME-002', 'HOME-003', 'HOME-004', 'HOME-005', 'HOME-006', 'HOME-007', 'HOME-008', 'HOME-009', 'HOME-010', 'HOME-011', 'HOME-012', 'HOME-013', 'HOME-014', 'HOME-015');
UPDATE templates SET status = 'ACTIVE', updated_at = now()
WHERE type = 'HOME_STATUS' AND status = 'RETIRED' AND code IN ('HOME-001', 'HOME-002', 'HOME-003', 'HOME-004', 'HOME-005', 'HOME-006', 'HOME-007', 'HOME-008', 'HOME-009', 'HOME-010', 'HOME-011', 'HOME-012', 'HOME-013', 'HOME-014', 'HOME-015');
