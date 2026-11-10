package com.ferry.system.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.system.api.dto.query.TenantQuery;
import com.ferry.system.api.entity.Tenant;
import com.ferry.system.api.vo.TenantVO;

import java.util.Date;

public class TenantWrapper extends BaseEntityWrapper<Tenant, TenantVO> {
    public static TenantWrapper build() {
        return new TenantWrapper();
    }


    @Override
    public TenantVO vo(Tenant entity) {
        return BeanUtil.copy(entity, TenantVO.class);
    }

    public LambdaQueryWrapper<Tenant> lambda(TenantQuery query) {
        LambdaQueryWrapper<Tenant> queryWrapper = Wrappers.<Tenant>lambdaQuery();
        String code = query.getCode();
        queryWrapper.like(StrUtil.isNotBlank(code), Tenant::getCode, code);
        String name = query.getName();
        queryWrapper.like(StrUtil.isNotBlank(name), Tenant::getName, name);
        Integer status = query.getStatus();
        queryWrapper.eq(ObjUtil.isNotEmpty(status), Tenant::getStatus, status);
        Date[] createTime = query.getCreateTime();
        if (createTime != null && createTime.length >= IntConst.TWO) {
            queryWrapper.between(Tenant::getCreateTime, createTime[IntConst.ZERO], createTime[IntConst.ONE]);
        }
        queryWrapper.orderByAsc(Tenant::getCode).orderByDesc(Tenant::getCreateTime);
        return queryWrapper;
    }
}
