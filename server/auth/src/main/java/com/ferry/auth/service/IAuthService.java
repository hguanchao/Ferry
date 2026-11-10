package com.ferry.auth.service;


import com.ferry.common.security.model.AuthInfo;

public interface IAuthService {

    /**
     * 登录
     *
     * @param tenantId 租户id
     * @param account  账号
     * @param password 密码
     * @return {@link AuthInfo} 认证信息
     */
    AuthInfo login(String tenantId, String account, String password);
}
