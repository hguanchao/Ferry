package com.ferry.devops.biz.feign;

import com.ferry.devops.api.feign.IAppFeign;
import com.ferry.devops.biz.service.IAppOwnerService;
import com.ferry.devops.biz.service.IAppService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 应用服务内部契约的服务端实现
 */
@RestController
@RequestMapping("/internal/app")
@AllArgsConstructor
public class AppFeignController implements IAppFeign {
    private final IAppService appService;
    private final IAppOwnerService ownerService;

    @Override
    public Map<String, String> appOptions() {
        return appService.optionMap();
    }

    @Override
    public List<Long> appOwners(Long appId) {
        return ownerService.ownerIds(appId);
    }
}
