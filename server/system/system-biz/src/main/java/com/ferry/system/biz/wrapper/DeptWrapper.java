package com.ferry.system.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.system.api.dto.query.DeptQuery;
import com.ferry.system.api.entity.Dept;
import com.ferry.system.api.vo.tree.DeptTree;

public class DeptWrapper extends BaseEntityWrapper<Dept, DeptTree> {
    public static DeptWrapper build() {
        return new DeptWrapper();
    }

    @Override
    public DeptTree vo(Dept entity) {
        return BeanUtil.copy(entity, DeptTree.class);
    }

    public LambdaQueryWrapper<Dept> lambda(DeptQuery query) {
        String tenantId = query.getTenantId();
        String name = query.getName();
        Integer status = query.getStatus();
        return Wrappers.<Dept>lambdaQuery()
                .eq(StrUtil.isNotBlank(tenantId), Dept::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(name), Dept::getName, name)
                .eq(ObjUtil.isNotEmpty(status), Dept::getStatus, status)
                .orderByAsc(Dept::getSort).orderByDesc(Dept::getCreateTime);
    }
}
