package com.ferry.auth.service.impl;

import com.ferry.auth.service.IAuthService;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.constant.SysConst;
import com.ferry.common.core.exception.AuthException;
import com.ferry.common.core.utils.CryptoUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.security.model.AuthInfo;
import com.ferry.common.security.model.AuthUser;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.common.web.utils.WebUtil;
import com.ferry.system.api.feign.ISystemFeign;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 密码认证
 */
@Service("password")
@RequiredArgsConstructor
public class PasswordAuthServiceImpl implements IAuthService {
    private final ISystemFeign systemFeign;

    @Override
    public AuthInfo login(String tenantId, String account, String password) {
        if (StrUtil.isBlank(tenantId)) {
            tenantId = WebUtil.getRequest().getHeader(SysConst.TENANT_ID);
        }
        AuthUtil.checkLoginLock(tenantId, account);
        AuthUser authUser = systemFeign.userAuth(tenantId, account);
        if (authUser == null || !CryptoUtil.matchesPassword(password, authUser.getPassword())) {
            AuthUtil.loginLock(tenantId, account);
            throw new AuthException("账号或密码错误");
        }
        if (IntConst.ZERO.equals(authUser.getStatus())) {
            throw new AuthException("当前用户已被禁用");
        }
        AuthUtil.loginUnlock(tenantId, account);
        return AuthUtil.login(authUser);
    }
}
