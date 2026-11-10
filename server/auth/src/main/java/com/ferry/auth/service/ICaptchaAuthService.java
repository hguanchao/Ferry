package com.ferry.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface ICaptchaAuthService {

    /**
     * 认证管理-获取图形验证码
     *
     * @param request 请求体
     * @return Map 验证码base64
     */
    Map<String, String> graphicsCaptcha(HttpServletRequest request);

    /**
     * 认证管理-发送手机验证码
     *
     * @param request 请求体
     * @return Map
     */
    Map<String, String> sendPhoneCaptcha(HttpServletRequest request);
}
