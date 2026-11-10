package com.ferry.system.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.system.api.dto.query.RoleQuery;
import com.ferry.system.api.entity.Role;
import com.ferry.system.api.vo.RoleVO;

public class RoleWrapper extends BaseEntityWrapper<Role, RoleVO> {
    public static RoleWrapper build() {
        return new RoleWrapper();
    }

    @Override
    public RoleVO vo(Role entity) {
        return BeanUtil.copy(entity, RoleVO.class);
    }

    public LambdaQueryWrapper<Role> lambda(RoleQuery query) {
        String tenantId = query.getTenantId();
        String code = query.getCode();
        String name = query.getName();
        Integer status = query.getStatus();
        return Wrappers.<Role>lambdaQuery()
                .eq(StrUtil.isNotBlank(tenantId), Role::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(code), Role::getCode, code)
                .like(StrUtil.isNotBlank(name), Role::getName, name)
                .eq(ObjUtil.isNotEmpty(status), Role::getStatus, status)
                .orderByAsc(Role::getSort).orderByDesc(Role::getCreateTime);
    }
}
