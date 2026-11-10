package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.exception.AuthException;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.http.HttpStatus;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.entity.Menu;
import com.ferry.system.api.entity.RoleMenu;
import com.ferry.system.biz.mapper.RoleMenuMapper;
import com.ferry.system.biz.service.IRoleMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu> implements IRoleMenuService {

    @Override
    public List<Menu> roleMenus(Long roleId) {
        return roleMenus(CollUtil.asList(roleId));
    }

    @Override
    public List<Menu> roleMenus(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return baseMapper.selectRoleMenuListByRoleId(StrUtil.join(roleIds));
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean authorizeMenu(RoleAuthorize<Long, Long> body) {
        // 给角色挂菜单等同于分配权限，只允许超管执行
        if (!AuthUtil.isSuper()) {
            throw new AuthException(HttpStatus.FORBIDDEN);
        }
        List<Long> roleIds = body.getAuthorizedIds();
        List<Long> menuIds = body.getAuthorizeIds();
        if (CollUtil.isEmpty(roleIds) || CollUtil.isEmpty(menuIds)) {
            throw new ServiceException("请选择要授权的角色或菜单");
        }
        remove(Wrappers.<RoleMenu>lambdaQuery().in(RoleMenu::getRoleId, roleIds));
        List<RoleMenu> menuRoleList = new ArrayList<>();
        menuIds.forEach(menuId -> roleIds.forEach(roleId -> menuRoleList.add(new RoleMenu(roleId, menuId))));
        return saveBatch(menuRoleList);
    }
}
