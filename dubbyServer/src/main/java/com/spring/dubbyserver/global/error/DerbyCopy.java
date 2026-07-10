package com.spring.dubbyserver.global.error;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ErrorCode → derbyMessage(사용자 노출 더비 톤 문구) 매핑.
 * 코드당 1~3개 문구 중 랜덤 선택 (반복 노출 완화). DB 조회 없음.
 * 장난 금지 구역(AUTH_*, BILLING_*, 계정 삭제)은 명확한 안내문 1개만 둔다.
 */
public final class DerbyCopy {

    private static final Map<ErrorCode, String[]> COPY = new EnumMap<>(ErrorCode.class);

    static {
        // COMMON
        COPY.put(ErrorCode.COMMON_INVALID_REQUEST, new String[]{
                "더비가 요청을 읽다가 길을 잃었습니다. 요청이 조금 이상한 것 같습니다.",
                "요청을 이해하지 못했습니다. 더비 탓일 확률과 요청 탓일 확률을 계산 중입니다."});
        COPY.put(ErrorCode.COMMON_NOT_FOUND, new String[]{
                "그런 것은 더비에게 없습니다. 원래 없었을 가능성도 검토 중입니다.",
                "찾아봤지만 없었습니다. 찾는 척은 확실히 했습니다."});
        COPY.put(ErrorCode.COMMON_INTERNAL_ERROR, new String[]{
                "더비는 이 상황을 예상하지 못했지만, 예상한 척하고 있습니다. 잠시 후 다시 시도해주세요.",
                "서버에서 무언가 떨어지는 소리가 났습니다. 더비가 줍는 중입니다."});

        // AUTH — 장난 금지: 명확한 안내
        COPY.put(ErrorCode.AUTH_TOKEN_INVALID, new String[]{
                "로그인 정보가 올바르지 않습니다. 앱을 다시 시작하면 자동으로 연결됩니다."});
        COPY.put(ErrorCode.AUTH_TOKEN_EXPIRED, new String[]{
                "로그인이 만료되었습니다. 앱을 다시 시작하면 자동으로 연결됩니다."});
        COPY.put(ErrorCode.AUTH_INVALID_DEVICE_ID, new String[]{
                "기기 정보가 올바르지 않습니다. 앱을 다시 설치하면 해결될 수 있습니다."});
        COPY.put(ErrorCode.AUTH_INVALID_TIMEZONE, new String[]{
                "시간대 정보가 올바르지 않습니다."});

        // SETTINGS
        COPY.put(ErrorCode.SETTINGS_INVALID_TIMEZONE, new String[]{
                "그런 시간대는 지구에 없는 것 같습니다. 더비가 세계지도를 다시 확인했습니다."});
        COPY.put(ErrorCode.SETTINGS_NICKNAME_TOO_LONG, new String[]{
                "이름이 더비의 기억력을 초과했습니다. 조금만 줄여주세요."});
        COPY.put(ErrorCode.SETTINGS_TIMEZONE_CHANGE_TOO_OFTEN, new String[]{
                "더비가 시차 적응 중입니다. 시간대는 하루에 한 번만 바꿀 수 있습니다."});

        // TASK
        COPY.put(ErrorCode.TASK_ASSIGNMENT_NOT_FOUND, new String[]{
                "그 업무는 더비의 책상에 없습니다. 애초에 책상이 있었는지도 확인 중입니다."});
        COPY.put(ErrorCode.TASK_INVALID_REACTION, new String[]{
                "그런 반응은 더비가 처리할 수 없습니다. 더비의 감정 처리 능력은 4종류뿐입니다."});
        COPY.put(ErrorCode.TASK_RETRY_EXHAUSTED, new String[]{
                "더비가 이 업무에서 손을 뗐습니다. 전략적 후퇴입니다."});

        // CHAT
        COPY.put(ErrorCode.CHAT_LIMIT_EXCEEDED, new String[]{
                "더비가 과로로 쓰러졌습니다. 방금 3개의 문장을 생각하고 모든 에너지를 소진했습니다.",
                "더비가 과로로 쓰러졌습니다. 무료 더비는 하루에 생각을 많이 하면 위험합니다."});
        COPY.put(ErrorCode.CHAT_CONTENT_TOO_LONG, new String[]{
                "문장이 더비의 처리 용량을 초과했습니다. 더비 기준 500자입니다."});
        COPY.put(ErrorCode.CHAT_CONCURRENT_REQUEST, new String[]{
                "더비는 한 번에 한 가지 생각만 할 수 있습니다. 사실 그것도 벅찹니다."});
        COPY.put(ErrorCode.CHAT_MESSAGE_NOT_FOUND, new String[]{
                "그 대화는 더비의 기억에 없습니다. 더비의 기억력을 감안하면 자연스러운 일입니다."});

        // LLM
        COPY.put(ErrorCode.LLM_UPSTREAM_ERROR, new String[]{
                "답변 생성에 실패했습니다. 더비가 생각을 너무 멀리 보냈고 아직 돌아오지 않았습니다.",
                "더비가 서버에게 말을 걸었지만 무시당했습니다. 다시 시도해주세요."});
        COPY.put(ErrorCode.LLM_TIMEOUT, new String[]{
                "더비가 잠시 천장을 보고 있습니다. 다시 시도해주세요.",
                "생각이 너무 길어졌습니다. 더비가 생각을 끊는 법을 몰라서 그렇습니다. 다시 시도해주세요."});
        COPY.put(ErrorCode.LLM_BUDGET_EXHAUSTED, new String[]{
                "더비 사무실의 전기가 오늘치 다 떨어졌습니다. 내일 다시 출근합니다."});

        // DIARY
        COPY.put(ErrorCode.DIARY_CANDIDATE_NOT_FOUND, new String[]{
                "그 기억 조각은 이미 사라졌습니다. 더비가 적기 전에 까먹은 것 같습니다."});
        COPY.put(ErrorCode.DIARY_ENTRY_NOT_FOUND, new String[]{
                "그 일기는 더비에게 없습니다. 원래 없었을 가능성도 검토 중입니다."});
        COPY.put(ErrorCode.DIARY_SLOT_FULL, new String[]{
                "일기장이 꽉 찼습니다. 더비의 기억력은 유료로 확장됩니다."});
        COPY.put(ErrorCode.DIARY_REWRITE_LIMIT, new String[]{
                "더비가 오늘 일기를 너무 많이 고쳐 썼습니다. 손목이 아프다고 합니다. 손목은 없지만요."});
        COPY.put(ErrorCode.DIARY_PREMIUM_ONLY, new String[]{
                "이 기능은 월급을 받는 더비만 할 수 있습니다. 더비도 아쉬워하고 있습니다."});

        // PUSH
        COPY.put(ErrorCode.PUSH_INVALID_TOKEN, new String[]{
                "알림 주소가 올바르지 않습니다. 더비가 편지를 보낼 곳을 찾지 못했습니다."});
        COPY.put(ErrorCode.PUSH_INVALID_DAILY_COUNT, new String[]{
                "더비를 그만큼 자주 견딜 수는 없습니다. 하루 1~3회만 가능합니다."});
        COPY.put(ErrorCode.PUSH_LOG_NOT_FOUND, new String[]{
                "그 알림의 기록이 없습니다. 더비가 보냈다고 주장만 하는 중입니다."});

        // BILLING — 장난 금지: 명확한 안내
        COPY.put(ErrorCode.BILLING_WEBHOOK_UNAUTHORIZED, new String[]{
                "결제 정보 인증에 실패했습니다."});
        COPY.put(ErrorCode.BILLING_SYNC_FAILED, new String[]{
                "결제 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요. 문제가 계속되면 문의해주세요."});
    }

    private static final String FALLBACK = "더비에게 알 수 없는 일이 일어났습니다. 더비도 놀랐습니다.";

    private DerbyCopy() {}

    public static String pick(ErrorCode code) {
        String[] candidates = COPY.get(code);
        if (candidates == null || candidates.length == 0) {
            return FALLBACK;
        }
        return candidates[ThreadLocalRandom.current().nextInt(candidates.length)];
    }
}
