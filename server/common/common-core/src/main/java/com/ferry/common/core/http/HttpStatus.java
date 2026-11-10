package com.ferry.common.core.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HTTP状态码 枚举
 */
@Getter
@AllArgsConstructor
public enum HttpStatus {

    SUCCESS(200, "请求成功"),
    FAILURE(400, "请求失败"),
    BAD_REQUEST(400, "业务异常"),
    UN_AUTHORIZED(401, "请求未授权"),
    FORBIDDEN(403, "请求被禁止"),
    NOT_FOUND(404, "404 请求未找到"),
    METHOD_NOT_ALLOWED(405, "不支持当前请求方法"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持当前请求方式"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务器维护中..."),
    ;

    /**
     * code编码
     */
    final int code;

    /**
     * 中文信息描述
     */
    final String message;
}
