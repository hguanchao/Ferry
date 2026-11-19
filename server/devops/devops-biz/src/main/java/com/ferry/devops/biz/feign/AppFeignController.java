package com.ferry.devops.biz.feign;

import com.ferry.devops.api.feign.IAppFeign;
import com.ferry.devops.biz.service.IAppService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 应用服务内部契约的服务端实现
 */
@RestController
@RequestMapping("/internal/app")
@AllArgsConstructor
public class AppFeignController implements IAppFeign {
    private final IAppService appService;

    @Override
    public Map<String, String> appOptions() {
        return appService.optionMap();
    }
    // TODO(dev_app-schema): dev_app_owner 已删除，appOwners 内部接口移除；负责人改为 dev_app.owner_id 单值。
}
