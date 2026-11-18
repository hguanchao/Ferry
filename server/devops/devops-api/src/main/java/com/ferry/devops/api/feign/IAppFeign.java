package com.ferry.devops.api.feign;

import com.ferry.common.core.constant.SysConst;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 应用服务内部契约
 */
@FeignClient(name = SysConst.APPLICATION_DEVOPS_NAME, path = "/internal/app")
public interface IAppFeign {

    /**
     * 应用选项，供登录态与其他服务下拉使用
     *
     * @return 应用id到名称的映射
     */
    @GetMapping(value = "/options")
    Map<String, String> appOptions();

    /**
     * 查询应用负责人用户id
     *
     * @param appId 应用id
     * @return 用户id集合
     */
    @GetMapping(value = "/owner")
    List<Long> appOwners(@RequestParam("appId") Long appId);
}
