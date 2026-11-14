package com.ferry.system.api.feign;

import com.ferry.common.core.constant.SysConst;
import com.ferry.common.security.model.AuthUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 系统服务契约
 */
@FeignClient(name = SysConst.APPLICATION_SYSTEM_NAME, path = "/internal")
public interface ISystemFeign {

    /**
     * 获取认证用户信息
     *
     * @param tenantId 租户id
     * @param account  用户账号
     * @return 认证用户信息
     */
    @GetMapping(value = "/user/auth")
    AuthUser userAuth(@RequestParam("tenantId") String tenantId, @RequestParam("account") String account);
}
