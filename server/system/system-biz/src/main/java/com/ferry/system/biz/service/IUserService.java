package com.ferry.system.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.dto.UserDTO;
import com.ferry.system.api.dto.query.UserQuery;
import com.ferry.system.api.entity.User;
import com.ferry.system.api.vo.UserVO;
import com.ferry.system.api.vo.tree.MenuTree;

import java.util.List;

public interface IUserService extends IService<User> {

    /**
     * 根据租户id和用户账号获取用户信息
     *
     * @param tenantId 租户id
     * @param account  用户账号
     * @return 用户信息
     */
    User auth(String tenantId, String account);

    /**
     * 用户管理-分页列表
     *
     * @param body 查询条件
     * @return 分页列表
     */
    IPage<UserVO> pageList(UserQuery body);

    /**
     * 用户管理-详情信息
     *
     * @param id 用户id
     * @return 详情信息
     */
    UserVO detail(Long id);

    /**
     * 用户管理-编辑信息
     *
     * @param body 表单数据
     * @return 是否成功
     */
    boolean save(UserDTO body);

    /**
     * 用户管理-重置密码，恢复为系统默认密码，仅超管可执行
     *
     * @param id 用户id
     * @return 是否成功
     */
    boolean resetPassword(Long id);

    /**
     * 用户管理-删除信息
     *
     * @param ids 用户id，支持批量
     * @return 是否成功
     */
    boolean deleted(List<Long> ids);

    /**
     * 用户管理-用户授权角色
     *
     * @param body 表单数据
     * @return 是否成功
     */
    boolean authorizeRole(RoleAuthorize<Long, Long> body);

    /**
     * 用户管理-获取用户权限
     *
     * @param tenantId 租户id
     * @param id       用户id
     * @param isSuper  是否超管 0：否 1：是
     * @return 权限列表
     */
    List<MenuTree> permissions(String tenantId, Long id, Integer isSuper);
}
