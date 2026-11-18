package com.ferry.devops.biz.integration.deployer;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.devops.api.enums.DeployTypeEnum;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部署执行器工厂：按部署类型路由到对应执行器。
 */
@Component
@AllArgsConstructor
public class DeployerFactory {

    private final List<Deployer> deployers;

    /**
     * 按部署类型获取执行器
     */
    public Deployer get(Integer deployType) {
        Map<Integer, Deployer> map = new HashMap<>();
        for (Deployer deployer : deployers) {
            map.put(deployer.deployType(), deployer);
        }
        Deployer deployer = map.get(deployType);
        if (deployer == null) {
            DeployTypeEnum type = DeployTypeEnum.of(deployType);
            throw new ServiceException(type == null ? "部署类型不支持" : type.getDesc() + "执行器未就绪");
        }
        return deployer;
    }
}
