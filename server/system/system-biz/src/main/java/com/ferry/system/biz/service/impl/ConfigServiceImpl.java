package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.system.api.dto.ConfigDTO;
import com.ferry.system.api.dto.query.ConfigQuery;
import com.ferry.system.api.entity.Config;
import com.ferry.system.api.vo.ConfigVO;
import com.ferry.system.biz.mapper.ConfigMapper;
import com.ferry.system.biz.service.IConfigService;
import com.ferry.system.biz.wrapper.ConfigWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements IConfigService {

    @Override
    public IPage<ConfigVO> pageList(ConfigQuery body) {
        ConfigWrapper wrapper = ConfigWrapper.build();
        IPage<Config> pages = page(Page.create(body), wrapper.lambda(body));
        return wrapper.pageVo(pages);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ConfigDTO body) {
        Config config = BeanUtil.copy(body, Config.class);
        // 校验配置键名唯一，重复会导致按键取值不确定
        long count = count(Wrappers.<Config>lambdaQuery()
                .eq(Config::getCode, config.getCode())
                .ne(ObjUtil.isNotEmpty(config.getId()), Config::getId, config.getId()));
        if (count > IntConst.ZERO) {
            throw new ServiceException("配置键名[" + config.getCode() + "]已存在");
        }
        return saveOrUpdate(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的配置");
        }
        return removeByIds(ids);
    }
}
