package com.ferry.system.biz.service;

import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.entity.Menu;
import com.ferry.system.api.entity.RoleMenu;

import java.util.List;

public interface IRoleMenuService extends IService<RoleMenu> {

    /**
     * 角色菜单列表
     *
     * @param roleId 角色id
     * @return 菜单列表
     */
    List<Menu> roleMenus(Long roleId);

    /**
     * 角色菜单列表
     *
     * @param roleIds 角色id集合
     * @return 菜单列表
     */
    List<Menu> roleMenus(List<Long> roleIds);

    /**
     * 角色授权菜单
     *
     * @param body 参数体
     * @return 是否成功
     */
    boolean authorizeMenu(RoleAuthorize<Long, Long> body);
}
