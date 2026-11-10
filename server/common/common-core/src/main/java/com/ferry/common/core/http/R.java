package com.ferry.common.core.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一API响应结果封装
 */
@Data
@NoArgsConstructor
@Tag(name = "返回信息")
public class R<T> implements Serializable {
    public static final String NO_DATA = "暂无数据";

    @Schema(description = "状态码")
    private int code;

    @Schema(description = "承载数据")
    private T data;

    @Schema(description = "返回消息")
    private String msg;

    private R(HttpStatus httpStatus) {
        this(httpStatus, null, httpStatus.getMessage());
    }

    private R(HttpStatus httpStatus, String msg) {
        this(httpStatus, null, msg);
    }

    private R(HttpStatus httpStatus, T data) {
        this(httpStatus, data, httpStatus.getMessage());
    }

    private R(HttpStatus httpStatus, T data, String msg) {
        this(httpStatus.getCode(), data, msg);
    }

    private R(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    /**
     * 判断返回是否为成功
     *
     * @return 是否成功
     */
    @JsonIgnore
    public boolean isSuccess() {
        return this.code == 200;
    }

    /**
     * 返回R
     *
     * @param data 数据
     * @param <T>  T 泛型标记
     * @return R
     */
    public static <T> R<T> data(T data) {
        return data(data, null);
    }

    /**
     * 返回R
     *
     * @param data 数据
     * @param msg  消息
     * @param <T>  T 泛型标记
     * @return R
     */
    public static <T> R<T> data(T data, String msg) {
        return data(HttpStatus.SUCCESS.getCode(), data, data == null ? NO_DATA : msg);
    }

    /**
     * 返回R
     *
     * @param code 状态码
     * @param data 数据
     * @param msg  消息
     * @param <T>  T 泛型标记
     * @return R
     */
    public static <T> R<T> data(int code, T data, String msg) {
        return new R<>(code, data, data == null ? NO_DATA : msg);
    }

    /**
     * 返回R
     *
     * @param <T> T 泛型标记
     * @return R
     */
    public static <T> R<T> success() {
        return new R<>(HttpStatus.SUCCESS);
    }

    /**
     * 返回R
     *
     * @param msg 消息
     * @param <T> T 泛型标记
     * @return R
     */
    public static <T> R<T> success(String msg) {
        return new R<>(HttpStatus.SUCCESS, msg);
    }

    /**
     * 返回R
     *
     * @param httpStatus 业务代码
     * @param <T>        T 泛型标记
     * @return R
     */
    public static <T> R<T> success(HttpStatus httpStatus) {
        return new R<>(httpStatus);
    }

    /**
     * 返回R
     *
     * @param httpStatus 业务代码
     * @param msg        消息
     * @param <T>        T 泛型标记
     * @return R
     */
    public static <T> R<T> success(HttpStatus httpStatus, String msg) {
        return new R<>(httpStatus, msg);
    }

    /**
     * 返回R
     *
     * @param <T> T 泛型标记
     * @return R
     */
    public static <T> R<T> fail() {
        return new R<>(HttpStatus.FAILURE);
    }

    /**
     * 返回R
     *
     * @param msg 消息
     * @param <T> T 泛型标记
     * @return R
     */
    public static <T> R<T> fail(String msg) {
        return new R<>(HttpStatus.FAILURE, msg);
    }


    /**
     * 返回R
     *
     * @param code 状态码
     * @param msg  消息
     * @param <T>  T 泛型标记
     * @return R
     */
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, null, msg);
    }

    /**
     * 返回R
     *
     * @param httpStatus 业务代码
     * @param <T>        T 泛型标记
     * @return R
     */
    public static <T> R<T> fail(HttpStatus httpStatus) {
        return new R<>(httpStatus);
    }

    /**
     * 返回R
     *
     * @param httpStatus 业务代码
     * @param msg        消息
     * @param <T>        T 泛型标记
     * @return R
     */
    public static <T> R<T> fail(HttpStatus httpStatus, String msg) {
        return new R<>(httpStatus, msg);
    }

    /**
     * 返回R
     *
     * @param flag 成功状态
     * @return R
     */
    public static <T> R<T> status(boolean flag) {
        return flag ? success(HttpStatus.SUCCESS) : fail(HttpStatus.FAILURE);
    }

}
