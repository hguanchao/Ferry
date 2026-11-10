package com.ferry.system.biz.service;

import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.entity.Role;
import com.ferry.system.api.entity.UserRole;

import java.util.List;

public interface IUserRoleService extends IService<UserRole> {

    /**
     * 角色列表
     *
     * @param roleIds 角色id(支持批量查询)
     * @return List 角色列表
     */
    List<Role> roles(List<Long> roleIds);

    /**
     * 用户角色列表
     *
     * @param userIds 用户id(支持批量查询)
     * @return List 角色列表
     */
    List<Role> userRoles(List<Long> userIds);

    /**
     * 用户授权角色
     *
     * @param body 参数体
     * @return true 成功 || false 失败
     */
    boolean authorizeRole(RoleAuthorize<Long, Long> body);
}
