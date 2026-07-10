package com.spring.dubbyserver.global.error;

/** 공통 에러 응답 { code, message, derbyMessage } — derby_system_spec_v1.md §6.1 */
public record ErrorResponse(String code, String message, String derbyMessage) {

    public static ErrorResponse of(ErrorCode code) {
        return new ErrorResponse(code.name(), code.getDefaultMessage(), DerbyCopy.pick(code));
    }

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code.name(), message, DerbyCopy.pick(code));
    }
}
