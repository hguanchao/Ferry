package com.ferry.auth.controller;

import com.ferry.auth.model.dto.LoginDTO;
import com.ferry.auth.service.IAuthService;
import com.ferry.auth.service.ICaptchaAuthService;
import com.ferry.common.core.exception.AuthException;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.SpringUtil;
import com.ferry.common.security.model.AuthInfo;
import com.ferry.common.security.utils.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "认证服务", description = "认证服务API接口控制器")
public class AuthController {
    private final ICaptchaAuthService captchaAuthService;

    @PostMapping("/login")
    @Operation(summary = "认证服务-登录", description = "传入账号和密码")
    public R<AuthInfo> login(@Valid @RequestBody LoginDTO body) {
        IAuthService authService = SpringUtil.getBean(body.getGrantType());
        if (authService == null) {
            throw new AuthException("登录失败，授权类型不正确");
        }
        return R.data(authService.login(body.getTenantId(), body.getAccount(), body.getPassword()));
    }

    @PostMapping("/logout")
    @Operation(summary = "认证服务-退出登录")
    public R<String> logout(HttpServletRequest request) {
        boolean isSuccess = AuthUtil.logout(request);
        return isSuccess ? R.success("退出成功") : R.fail("退出失败");
    }

    @GetMapping("/{type}/captcha")
    @Operation(summary = "认证服务-获取验证码")
    public R<Map<String, String>> captcha(@PathVariable("type") Integer type, HttpServletRequest request) {
        if (ObjUtil.equals(1, type)) {
            // 图形验证码
            return R.data(captchaAuthService.graphicsCaptcha(request));
        } else {
            // 手机验证码
            return R.data(captchaAuthService.sendPhoneCaptcha(request));
        }
    }
}
