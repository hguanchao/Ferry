package com.ferry.devops.api.feign;

import com.ferry.common.core.constant.SysConst;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 交付服务契约
 */
@FeignClient(name = SysConst.APPLICATION_DEVOPS_NAME, path = "/internal")
public interface IDevopsFeign {

    /**
     * 应用选项，供登录态与其他服务下拉使用
     *
     * @return 应用id到名称的映射
     */
    @GetMapping(value = "/app/options")
    Map<String, String> appOptions();

    /**
     * 查询应用负责人用户id
     *
     * @param appId 应用id
     * @return 用户id集合
     */
    @GetMapping(value = "/app/owner")
    List<Long> appOwners(@RequestParam("appId") Long appId);

    /**
     * 环境选项，按应用过滤
     *
     * @param appId 应用id
     * @return 环境id到名称的映射
     */
    @GetMapping(value = "/environment/options")
    Map<String, String> environmentOptions(@RequestParam(value = "appId", required = false) Long appId);
}
