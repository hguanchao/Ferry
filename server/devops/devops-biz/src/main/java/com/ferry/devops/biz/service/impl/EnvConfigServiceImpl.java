package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.dto.EnvConfigDTO;
import com.ferry.devops.api.entity.EnvConfig;
import com.ferry.devops.api.entity.Environment;
import com.ferry.devops.api.vo.EnvConfigVO;
import com.ferry.devops.biz.mapper.EnvConfigMapper;
import com.ferry.devops.biz.mapper.EnvironmentMapper;
import com.ferry.devops.biz.service.IEnvConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境配置管理服务实现
 */
@Service
@RequiredArgsConstructor
public class EnvConfigServiceImpl extends ServiceImpl<EnvConfigMapper, EnvConfig> implements IEnvConfigService {

    private static final String MASK = "******";

    private final EnvironmentMapper environmentMapper;

    @Override
    public List<EnvConfigVO> listByEnv(Long envId) {
        return mask(BeanUtil.copy(list(Wrappers.<EnvConfig>lambdaQuery()
                .eq(EnvConfig::getEnvId, envId)
                .orderByAsc(EnvConfig::getConfigKey)), EnvConfigVO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(EnvConfigDTO body) {
        long count = count(Wrappers.<EnvConfig>lambdaQuery()
                .eq(EnvConfig::getEnvId, body.getEnvId())
                .eq(EnvConfig::getConfigKey, body.getConfigKey())
                .ne(ObjUtil.isNotEmpty(body.getId()), EnvConfig::getId, body.getId()));
        if (count > IntConst.ZERO) {
            throw new ServiceException("同一环境下配置键已存在");
        }
        EnvConfig config = BeanUtil.copy(body, EnvConfig.class);
        int version = ObjUtil.isEmpty(body.getId())
                ? IntConst.ONE
                : requireConfig(body.getId()).getVersion() + IntConst.ONE;
        config.setVersion(version);
        boolean ok = saveOrUpdate(config);

        // 同步环境配置版本
        Environment env = environmentMapper.selectById(body.getEnvId());
        if (env == null) {
            throw new ServiceException("环境不存在");
        }
        env.setConfigVersion(version);
        environmentMapper.updateById(env);
        return ok;
    }

    @Override
    public List<Map<String, Object>> diff(Long envId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (EnvConfig config : list(Wrappers.<EnvConfig>lambdaQuery().eq(EnvConfig::getEnvId, envId))) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("configKey", config.getConfigKey());
            item.put("valueType", config.getValueType());
            item.put("version", config.getVersion());
            // 敏感配置读取时返回掩码，存储为明文
            item.put("configValue", ObjUtil.equals(config.getValueType(), IntConst.TWO)
                    ? "******" : StrUtil.isBlank(config.getConfigValue()) ? "" : config.getConfigValue());
            item.put("changed", true);
            result.add(item);
        }
        return result;
    }

    private EnvConfig requireConfig(Long id) {
        EnvConfig config = getById(id);
        if (config == null) {
            throw new ServiceException("配置不存在");
        }
        return config;
    }

    private List<EnvConfigVO> mask(List<EnvConfigVO> items) {
        for (EnvConfigVO vo : items) {
            if (ObjUtil.equals(vo.getValueType(), IntConst.TWO) || ObjUtil.equals(vo.getIsEncrypted(), IntConst.ONE)) {
                vo.setConfigValue(MASK);
            }
        }
        return items;
    }
}
