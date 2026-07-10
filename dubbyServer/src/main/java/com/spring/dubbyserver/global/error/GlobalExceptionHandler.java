package com.spring.dubbyserver.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DubbyException.class)
    public ResponseEntity<ErrorResponse> handleDubby(DubbyException e) {
        ErrorCode code = e.getErrorCode();
        if (code.getStatus().is5xxServerError()) {
            log.error("DubbyException [{}]: {}", code.name(), e.getMessage(), e);
        } else {
            log.debug("DubbyException [{}]: {}", code.name(), e.getMessage());
        }
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse(ErrorCode.COMMON_INVALID_REQUEST.getDefaultMessage());
        return ResponseEntity.status(ErrorCode.COMMON_INVALID_REQUEST.getStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_INVALID_REQUEST, detail));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(ErrorCode.COMMON_INVALID_REQUEST.getStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_INVALID_REQUEST, "Malformed request body"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(ErrorCode.COMMON_NOT_FOUND.getStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ErrorCode.COMMON_INTERNAL_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_INTERNAL_ERROR));
    }
}
