package com.spring.dubbyserver.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 체계 (derby_system_spec_v1.md §6).
 * code 문자열(enum 이름)은 클라이언트 분기 계약 — 이름 변경 금지.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // COMMON
    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),

    // AUTH (장난 금지 구역)
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Invalid access token"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access token expired"),
    AUTH_INVALID_DEVICE_ID(HttpStatus.BAD_REQUEST, "deviceId must be a UUID"),
    AUTH_INVALID_TIMEZONE(HttpStatus.BAD_REQUEST, "timezone must be a valid IANA zone id"),

    // SETTINGS
    SETTINGS_INVALID_TIMEZONE(HttpStatus.BAD_REQUEST, "timezone must be a valid IANA zone id"),
    SETTINGS_NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "nickname too long"),
    SETTINGS_TIMEZONE_CHANGE_TOO_OFTEN(HttpStatus.TOO_MANY_REQUESTS, "timezone can be changed once per interval"),

    // TASK
    TASK_ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Task assignment not found"),
    TASK_INVALID_REACTION(HttpStatus.BAD_REQUEST, "Invalid reaction"),
    TASK_RETRY_EXHAUSTED(HttpStatus.CONFLICT, "Retry limit exhausted"),

    // CHAT
    CHAT_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Daily chat limit reached"),
    CHAT_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "Content too long"),
    CHAT_CONCURRENT_REQUEST(HttpStatus.TOO_MANY_REQUESTS, "Another chat request is in progress"),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Chat message not found"),

    // LLM
    LLM_UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "LLM provider request failed"),
    LLM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "LLM provider timeout"),
    LLM_BUDGET_EXHAUSTED(HttpStatus.SERVICE_UNAVAILABLE, "Global daily LLM budget exhausted"),

    // DIARY
    DIARY_CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "Diary candidate not found or expired"),
    DIARY_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "Diary entry not found"),
    DIARY_SLOT_FULL(HttpStatus.CONFLICT, "Diary slot limit reached"),
    DIARY_REWRITE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "Daily rewrite limit reached"),
    DIARY_PREMIUM_ONLY(HttpStatus.FORBIDDEN, "Premium feature"),

    // PUSH
    PUSH_INVALID_TOKEN(HttpStatus.BAD_REQUEST, "Invalid Expo push token"),
    PUSH_INVALID_DAILY_COUNT(HttpStatus.BAD_REQUEST, "Invalid daily push count"),
    PUSH_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "Push log not found"),

    // BILLING (장난 금지 구역)
    BILLING_WEBHOOK_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Invalid webhook secret"),
    BILLING_SYNC_FAILED(HttpStatus.BAD_GATEWAY, "RevenueCat sync failed");

    private final HttpStatus status;
    private final String defaultMessage;
}
