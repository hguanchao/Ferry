package com.ferry.devops.biz.feign;

import com.ferry.devops.api.feign.IDevopsFeign;
import com.ferry.devops.biz.service.IAppService;
import com.ferry.devops.biz.service.IEnvironmentService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 交付服务契约的服务端实现
 */
@RestController
@RequestMapping("/internal")
@AllArgsConstructor
public class DevopsFeign implements IDevopsFeign {
    private final IAppService appService;
    private final IEnvironmentService environmentService;

    @Override
    public Map<String, String> appOptions() {
        return appService.optionMap();
    }

    @Override
    public List<Long> appOwners(Long appId) {
        return appService.owners(appId);
    }

    @Override
    public Map<String, String> environmentOptions(Long appId) {
        return environmentService.optionMap(appId);
    }
}
