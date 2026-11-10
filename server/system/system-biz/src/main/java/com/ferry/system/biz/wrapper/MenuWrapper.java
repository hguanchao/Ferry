package com.ferry.system.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.system.api.dto.query.MenuQuery;
import com.ferry.system.api.entity.Menu;
import com.ferry.system.api.vo.tree.MenuTree;

public class MenuWrapper extends BaseEntityWrapper<Menu, MenuTree> {
    public static MenuWrapper build() {
        return new MenuWrapper();
    }

    @Override
    public MenuTree vo(Menu entity) {
        return BeanUtil.copy(entity, MenuTree.class);
    }

    public LambdaQueryWrapper<Menu> lambda(MenuQuery query) {
        String tenantId = query.getTenantId();
        String name = query.getName();
        Integer type = query.getType();
        Integer status = query.getStatus();
        return Wrappers.<Menu>lambdaQuery()
                .eq(StrUtil.isNotBlank(tenantId), Menu::getTenantId, tenantId)
                .like(StrUtil.isNotBlank(name), Menu::getName, name)
                .eq(ObjUtil.isNotEmpty(type), Menu::getType, type)
                .eq(ObjUtil.isNotEmpty(status), Menu::getStatus, status)
                .orderByAsc(Menu::getSort).orderByDesc(Menu::getCreateTime);
    }
}
