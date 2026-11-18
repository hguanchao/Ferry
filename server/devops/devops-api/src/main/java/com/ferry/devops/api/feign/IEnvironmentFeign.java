package com.ferry.devops.api.feign;

import com.ferry.common.core.constant.SysConst;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 环境服务内部契约
 */
@FeignClient(name = SysConst.APPLICATION_DEVOPS_NAME, path = "/internal/environment")
public interface IEnvironmentFeign {

    /**
     * 环境选项，按应用过滤
     *
     * @param appId 应用id
     * @return 环境id到名称的映射
     */
    @GetMapping(value = "/options")
    Map<String, String> environmentOptions(@RequestParam(value = "appId", required = false) Long appId);
}
