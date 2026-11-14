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
import com.ferry.system.api.dto.TenantDTO;
import com.ferry.system.api.dto.query.TenantQuery;
import com.ferry.system.api.entity.Tenant;
import com.ferry.system.api.vo.TenantVO;
import com.ferry.system.biz.mapper.TenantMapper;
import com.ferry.system.biz.service.ITenantService;
import com.ferry.system.biz.wrapper.TenantWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements ITenantService {

    @Override
    public List<TenantVO> list(TenantQuery body) {
        List<Tenant> dataList = list(TenantWrapper.build().lambda(body));
        return BeanUtil.copy(dataList, TenantVO.class);
    }

    @Override
    public IPage<TenantVO> pageList(TenantQuery body) {
        return baseMapper.selectPageList(Page.create(body), body);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TenantDTO body) {
        Tenant tenant = BeanUtil.copy(body, Tenant.class);
        // 校验租户编码唯一，重复会导致登录归属错乱
        long count = count(Wrappers.<Tenant>lambdaQuery()
                .eq(Tenant::getCode, tenant.getCode())
                .ne(ObjUtil.isNotEmpty(tenant.getId()), Tenant::getId, tenant.getId()));
        if (count > IntConst.ZERO) {
            throw new ServiceException("租户编码[" + tenant.getCode() + "]已存在");
        }
        return saveOrUpdate(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的租户");
        }
        return removeByIds(ids);
    }
}
