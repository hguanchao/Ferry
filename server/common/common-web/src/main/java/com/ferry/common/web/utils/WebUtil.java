package com.ferry.common.web.utils;

import com.ferry.common.core.constant.StrConst;
import com.ferry.common.core.exception.UtilException;
import com.ferry.common.core.utils.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Web工具类
 */
public class WebUtil extends WebUtils {
    public static final String UN_KNOWN = "unknown";

    /**
     * 获取 HttpServletRequest
     *
     * @return 当前请求对象
     */
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Assert.notNull(requestAttributes, "The RequestAttributes is null");
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    /**
     * 获得请求header中的信息
     *
     * @param name 头信息的KEY
     * @return header值
     */
    public static String getHeader(String name) {
        return getHeader(getRequest(), name);
    }

    /**
     * 获得请求header中的信息
     *
     * @param request 请求对象{@link HttpServletRequest}
     * @param name    头信息的KEY
     * @return header值
     */
    public static String getHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (StrUtil.isBlank(value)) {
            return StrConst.EMPTY;
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    /**
     * 获取请求参数中的值
     *
     * @param name 参数名称
     * @return 参数值
     */
    public static String getParameter(String name) {
        return getParameter(getRequest(), name);
    }

    /**
     * 获取请求参数中的值
     *
     * @param request 请求体
     * @param name    参数名称
     * @return 参数值
     */
    public static String getParameter(HttpServletRequest request, String name) {
        return request.getParameter(name);
    }

    /**
     * 获取ip
     *
     * @param request HttpServletRequest
     * @return IP
     */
    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Requested-For");
        if (StrUtil.isBlank(ip) || UN_KNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (StrUtil.isBlank(ip) || UN_KNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || UN_KNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || UN_KNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StrUtil.isBlank(ip) || UN_KNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StrUtil.isBlank(ip) || UN_KNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return StrUtil.isBlank(ip) ? null : ip.split(",")[0];
    }
}
