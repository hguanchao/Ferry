package com.ferry.system.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.system.api.dto.query.ConfigQuery;
import com.ferry.system.api.entity.Config;
import com.ferry.system.api.vo.ConfigVO;

public class ConfigWrapper extends BaseEntityWrapper<Config, ConfigVO> {
    public static ConfigWrapper build() {
        return new ConfigWrapper();
    }

    @Override
    public ConfigVO vo(Config entity) {
        return BeanUtil.copy(entity, ConfigVO.class);
    }

    public LambdaQueryWrapper<Config> lambda(ConfigQuery query) {
        String name = query.getName();
        String code = query.getCode();
        Integer isInternal = query.getIsInternal();
        return Wrappers.<Config>lambdaQuery()
                .like(StrUtil.isNotBlank(name), Config::getName, name)
                .like(StrUtil.isNotBlank(code), Config::getCode, code)
                .eq(ObjUtil.isNotEmpty(isInternal), Config::getIsInternal, isInternal)
                .orderByDesc(Config::getCreateTime);
    }
}
