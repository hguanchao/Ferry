package com.ferry.devops.api.feign;

import com.ferry.common.core.constant.SysConst;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 应用服务内部契约
 */
@FeignClient(name = SysConst.APPLICATION_DEVOPS_NAME, contextId = "devopsAppClient", path = "/internal/app")
public interface IAppFeign {

    /**
     * 应用选项，供登录态与其他服务下拉使用
     *
     * @return 应用id到名称的映射
     */
    @GetMapping(value = "/options")
    Map<String, String> appOptions();

    // TODO(dev_app-schema): dev_app_owner 已删除，appOwners 内部接口移除；负责人改为 dev_app.owner_id 单值。
}
