package com.ferry.auth.service.impl;

import com.ferry.auth.service.IAuthService;
import com.ferry.auth.service.ICaptchaAuthService;
import com.ferry.common.cache.utils.RedisUtil;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.constant.SysConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.NumUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.RegexUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.security.model.AuthInfo;
import com.ferry.common.web.utils.WebUtil;
import com.wf.captcha.SpecCaptcha;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 验证码认证
 */
@Slf4j
@Service("captcha")
@RequiredArgsConstructor
public class CaptchaAuthServiceImpl implements IAuthService, ICaptchaAuthService {
    private final PasswordAuthServiceImpl passwordAuthService;

    public static final String CAPTCHA_CODE = "Captcha-Code";
    public static final String MOBILE_PHONE = "Mobile-Phone";
    public static final String PHONE_CAPTCHA_CACHE_KEY = SysConst.AUTH_CACHE_PREFIX + "phoneCaptcha:";
    public static final String GRAPHICS_CAPTCHA_CACHE_KEY = SysConst.AUTH_CACHE_PREFIX + "graphicsCaptcha:";

    @Override
    public AuthInfo login(String tenantId, String account, String password) {
        HttpServletRequest request = WebUtil.getRequest();
        String code = request.getHeader(CAPTCHA_CODE);
        String key = GRAPHICS_CAPTCHA_CACHE_KEY + WebUtil.getIp(request);
        if (StrUtil.isBlank(code) || !ObjUtil.equals(code, RedisUtil.get(key))) {
            throw new ServiceException("验证码不正确或已过期");
        }
        // 校验通过后立即作废验证码，防止同一码在有效期内反复提交
        RedisUtil.del(key);
        return passwordAuthService.login(tenantId, account, password);
    }

    @Override
    public Map<String, String> sendPhoneCaptcha(HttpServletRequest request) {
        String code = StrUtil.toStr(NumUtil.randomInt(6));
        String phone = request.getHeader(MOBILE_PHONE);
        if (StrUtil.isBlank(phone) || !RegexUtil.isPhone(phone)) {
            throw new ServiceException("手机号码格式不正确");
        }
        String key = PHONE_CAPTCHA_CACHE_KEY + phone;
        boolean isSuccess = RedisUtil.set(key, code, IntConst.THREE, TimeUnit.MINUTES);
        if (!isSuccess) {
            throw new ServiceException("验证码发送失败");
        }
        // 验证码只经短信通道下发，不从响应体返回，防止取码后冒用
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> graphicsCaptcha(HttpServletRequest request) {
        try {
            SpecCaptcha specCaptcha = new SpecCaptcha(130, 48, 4);
            String captchaCode = specCaptcha.text().toLowerCase();
            String key = GRAPHICS_CAPTCHA_CACHE_KEY + WebUtil.getIp(request);
            RedisUtil.set(key, captchaCode, IntConst.THREE, TimeUnit.MINUTES);
            return CollUtil.asMap("captcha", specCaptcha.toBase64());
        } catch (Exception e) {
            log.error("Failed to generate captcha: {}", e.getMessage(), e);
            throw new ServiceException("获取验证码失败");
        }
    }
}
