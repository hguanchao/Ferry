package com.ferry.common.web.handler;

import com.ferry.common.core.exception.AuthException;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.exception.UtilException;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class ExceptionAdviceHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleError(MissingServletRequestParameterException e) {
        log.warn("Missing required parameter, name={}", e.getParameterName());
        String message = String.format("缺少参数: %s", e.getParameterName());
        return R.fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleError(MethodArgumentTypeMismatchException e) {
        log.warn("Parameter type mismatch, name={}", e.getName());
        String message = String.format("参数格式错误: %s", e.getName());
        return R.fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleError(MethodArgumentNotValidException e) {
        log.warn("Request body validation failed, fields={}", fieldNames(e.getBindingResult()));
        return handleError(e.getBindingResult());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleError(BindException e) {
        log.warn("Request parameter binding failed, fields={}", fieldNames(e.getBindingResult()));
        return handleError(e.getBindingResult());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleError(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        log.warn("Request parameter validation failed, paths={}",
                violations.stream().map(item -> item.getPropertyPath().toString()).toList());
        String message = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining("/"));
        return R.fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleError(HttpMessageNotReadableException e) {
        String message = e.getMessage();
        if (StrUtil.isNotBlank(message)) {
            log.error("Unreadable request body, detail={}", message);
            message = message.split(":")[0];
        }
        return R.fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<String> handleError(HttpRequestMethodNotSupportedException e) {
        log.error("HTTP method not allowed, method={}", e.getMethod());
        return R.fail(HttpStatus.METHOD_NOT_ALLOWED.value(), e.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public R<String> handleError(HttpMediaTypeNotSupportedException e) {
        log.error("Unsupported media type, contentType={}", e.getContentType());
        return R.fail(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), e.getMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<String> handleError(NoHandlerFoundException e) {
        log.error("No handler found, method={}, uri={}", e.getHttpMethod(), e.getRequestURL());
        return R.fail(HttpStatus.NOT_FOUND.value(), "404 请求未找到");
    }

    @ExceptionHandler(ServiceException.class)
    public R<String> handleError(ServiceException e, HttpServletResponse response) {
        // 状态码跟随异常自带的语义，但返回值保持 R，与其余处理器同一种出口格式
        response.setStatus(e.getHttpStatus().getCode());
        log.warn("Business error, status={}, message={}", e.getHttpStatus().getCode(), e.getMessage());
        return R.fail(e.getHttpStatus(), e.getMessage());
    }

    @ExceptionHandler(UtilException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<String> handleError(UtilException e) {
        log.error("Utility error, message={}", e.getMessage(), e);
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(AuthException.class)
    public R<String> handleError(AuthException e, HttpServletResponse response) {
        // 登录失效是 401、权限不足是 403，不能用 @ResponseStatus 压成同一个状态码
        response.setStatus(e.getHttpStatus().getCode());
        log.warn("Authentication failed, status={}, message={}", e.getHttpStatus().getCode(), e.getMessage());
        return R.fail(e.getHttpStatus(), e.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<String> handleError(Throwable e) {
        log.error("Unhandled exception", e);
        return R.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误");
    }

    private R<String> handleError(BindingResult result) {
        FieldError error = result.getFieldError();
        String message = String.format("%s：%s", error.getField(), error.getDefaultMessage());
        return R.fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 提取校验失败的字段名，日志定位到字段即可，无需打印整段校验消息
     *
     * @param result 绑定结果
     * @return 去重后的字段名
     */
    private List<String> fieldNames(BindingResult result) {
        return result.getFieldErrors().stream().map(FieldError::getField).distinct().toList();
    }
}
