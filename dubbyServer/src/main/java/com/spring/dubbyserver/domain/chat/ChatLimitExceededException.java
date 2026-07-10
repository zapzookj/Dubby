package com.spring.dubbyserver.domain.chat;

import com.spring.dubbyserver.global.error.DubbyException;
import com.spring.dubbyserver.global.error.ErrorCode;
import lombok.Getter;

/** 429 응답에 resetsAt/paywallHint 확장 필드를 싣기 위한 특수 예외 */
@Getter
public class ChatLimitExceededException extends DubbyException {

    private final String resetsAt;
    private final String paywallHint;

    public ChatLimitExceededException(String resetsAt, String paywallHint) {
        super(ErrorCode.CHAT_LIMIT_EXCEEDED);
        this.resetsAt = resetsAt;
        this.paywallHint = paywallHint;
    }
}
