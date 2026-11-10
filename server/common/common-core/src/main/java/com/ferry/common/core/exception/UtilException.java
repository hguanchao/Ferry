package com.ferry.common.core.exception;


import com.ferry.common.core.utils.StrUtil;

/**
 * 工具异常类
 */
public class UtilException extends RuntimeException {

    public UtilException(Throwable e) {
        super(StrUtil.format("{}: {}", e.getClass().getSimpleName(), e.getMessage()), e);
    }

    public UtilException(String message) {
        super(message);
    }

    public UtilException(String messageTemplate, Object... params) {
        super(StrUtil.format(messageTemplate, params));
    }

    public UtilException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public UtilException(String message, Throwable throwable, boolean enableSuppression, boolean writableStackTrace) {
        super(message, throwable, enableSuppression, writableStackTrace);
    }

    public UtilException(Throwable throwable, String messageTemplate, Object... params) {
        super(StrUtil.format(messageTemplate, params), throwable);
    }
}