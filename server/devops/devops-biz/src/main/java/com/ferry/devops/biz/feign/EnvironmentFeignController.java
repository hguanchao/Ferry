package com.ferry.devops.biz.feign;

import com.ferry.devops.api.feign.IEnvironmentFeign;
import com.ferry.devops.biz.service.IEnvironmentService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 环境服务内部契约的服务端实现
 */
@RestController
@RequestMapping("/internal/environment")
@AllArgsConstructor
public class EnvironmentFeignController implements IEnvironmentFeign {
    private final IEnvironmentService environmentService;

    @Override
    public Map<String, String> environmentOptions(Long appId) {
        return environmentService.optionMap(appId);
    }
}
