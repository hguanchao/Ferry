package com.ferry.common.core.exception;

import com.ferry.common.core.http.HttpStatus;
import lombok.Getter;

/**
 * 业务异常类
 */
public class ServiceException extends RuntimeException {
    @Getter
    private final HttpStatus httpStatus;

    public ServiceException(String message) {
        super(message);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public ServiceException(HttpStatus httpStatus) {
        super(httpStatus.getMessage());
        this.httpStatus = httpStatus;
    }

    public ServiceException(HttpStatus httpStatus, Throwable cause) {
        super(cause);
        this.httpStatus = httpStatus;
    }

    public ServiceException(Throwable cause) {
        super(cause);
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
