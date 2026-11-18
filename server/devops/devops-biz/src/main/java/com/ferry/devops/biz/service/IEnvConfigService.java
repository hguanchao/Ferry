package com.ferry.devops.biz.service;

import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.EnvConfigDTO;
import com.ferry.devops.api.entity.EnvConfig;
import com.ferry.devops.api.vo.EnvConfigVO;

import java.util.List;
import java.util.Map;

/**
 * 环境配置管理服务
 */
public interface IEnvConfigService extends IService<EnvConfig> {

    /**
     * 按环境查询配置列表（凭据值掩码）
     */
    List<EnvConfigVO> listByEnv(Long envId);

    /**
     * 保存配置，生成新版本并返回环境配置版本摘要
     */
    boolean save(EnvConfigDTO body);

    /**
     * 配置差异对比
     */
    List<Map<String, Object>> diff(Long envId);
}
