package com.ferry.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface ICaptchaAuthService {

    /**
     * 认证管理-获取图形验证码
     *
     * @param request 当前请求
     * @return 验证码图片 base64
     */
    Map<String, String> graphicsCaptcha(HttpServletRequest request);

    /**
     * 认证管理-发送手机验证码
     *
     * @param request 当前请求
     * @return 空集合，验证码仅经短信下发，不随响应返回
     */
    Map<String, String> sendPhoneCaptcha(HttpServletRequest request);
}
