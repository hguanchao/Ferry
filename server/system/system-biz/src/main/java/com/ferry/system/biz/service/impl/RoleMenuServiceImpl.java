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
    @Transactional(rollbackFor = Exception.class)
    public boolean authorizeMenu(RoleAuthorize<Long, Long> body) {
        // 给角色挂菜单等同于分配权限，只允许超管执行
        if (!AuthUtil.isSuper()) {
            throw new AuthException(HttpStatus.FORBIDDEN);
        }
        // authorizedIds 是「被授权对象」（角色），authorizeIds 是「授予的内容」（菜单）。
        // 这两个名字容易看反：写反会落成 (菜单id, 角色id)，查询按角色反查永远为空。
        List<Long> roleIds = body.getAuthorizedIds();
        List<Long> menuIds = body.getAuthorizeIds();
        if (CollUtil.isEmpty(roleIds)) {
            throw new ServiceException("请选择要授权的角色");
        }
        // 先清空再按新集合重建，因此空菜单是合法输入，表示撤销该角色的全部菜单授权
        remove(Wrappers.<RoleMenu>lambdaQuery().in(RoleMenu::getRoleId, roleIds));
        if (CollUtil.isEmpty(menuIds)) {
            return true;
        }
        List<RoleMenu> menuRoleList = new ArrayList<>();
        menuIds.forEach(menuId -> roleIds.forEach(roleId -> menuRoleList.add(new RoleMenu(roleId, menuId))));
        return saveBatch(menuRoleList);
    }
}
