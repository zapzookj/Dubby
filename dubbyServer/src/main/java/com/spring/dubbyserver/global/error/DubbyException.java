package com.spring.dubbyserver.global.error;

import lombok.Getter;

@Getter
public class DubbyException extends RuntimeException {

    private final ErrorCode errorCode;

    public DubbyException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public DubbyException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
