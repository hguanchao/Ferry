package com.ferry.common.core.exception;

import com.ferry.common.core.http.HttpStatus;
import lombok.Getter;

/**
 * 认证异常类
 */
public class AuthException extends RuntimeException {
    @Getter
    private final HttpStatus httpStatus;

    public AuthException(String message) {
        super(message);
        this.httpStatus = HttpStatus.UN_AUTHORIZED;
    }

    public AuthException(HttpStatus httpStatus) {
        super(httpStatus.getMessage());
        this.httpStatus = httpStatus;
    }

    public AuthException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public AuthException(HttpStatus httpStatus, Throwable cause) {
        super(cause);
        this.httpStatus = httpStatus;
    }

    public AuthException(Throwable cause) {
        super(cause);
        this.httpStatus = HttpStatus.UN_AUTHORIZED;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
