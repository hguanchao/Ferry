package com.ferry.system.api.feign;

import com.ferry.common.core.constant.SysConst;
import com.ferry.common.security.model.AuthUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 系统服务契约
 *
 * <p>统一声明在 /internal 下：path 属性会拼在每个方法的路径前，指向服务端的同名映射，
 * 网关据此前缀拒绝外部访问，新增方法自动纳入保护范围，无需另行登记。
 *
 * <p>前缀只能声明在 path 上：OpenFeign 的 SpringMvcContract 禁止 @FeignClient 接口
 * 标注类型级 @RequestMapping，写了会导致契约解析失败、服务无法启动。
 */
@FeignClient(name = SysConst.APPLICATION_SYSTEM_NAME, path = "/internal")
public interface ISystemFeign {

    /**
     * 获取认证用户信息
     *
     * @param tenantId 租户id
     * @param account  用户账号
     * @return {@link AuthUser} 认证用户信息
     */
    @GetMapping(value = "/user/auth")
    AuthUser userAuth(@RequestParam("tenantId") String tenantId, @RequestParam("account") String account);
}
