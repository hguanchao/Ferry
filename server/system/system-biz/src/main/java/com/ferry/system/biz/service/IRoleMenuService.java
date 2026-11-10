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
     * @return List 菜单列表
     */
    List<Menu> roleMenus(Long roleId);

    /**
     * 角色菜单列表
     *
     * @param roleIds 角色id(支持批量查询)
     * @return List 菜单列表
     */
    List<Menu> roleMenus(List<Long> roleIds);

    /**
     * 角色授权菜单
     *
     * @param body 参数体
     * @return true 成功 || false 失败
     */
    boolean authorizeMenu(RoleAuthorize<Long, Long> body);
}
