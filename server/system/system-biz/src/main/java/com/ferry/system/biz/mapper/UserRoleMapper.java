package com.ferry.system.biz.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.system.api.entity.Role;
import com.ferry.system.api.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 查询角色列表
     *
     * @param roleIds 角色id，多个按逗号拼接
     * @return 角色列表
     */
    List<Role> selectRoleListByRoleIds(@Param("roleIds") String roleIds);

    /**
     * 查询用户授权的角色列表
     *
     * <p>登录校验时没有租户上下文，跳过租户拦截器，查询按用户id收敛不会扩散到无关用户。
     *
     * @param userIds 用户id，多个按逗号拼接
     * @return 角色列表
     */
    @InterceptorIgnore(tenantLine = "true")
    List<Role> selectUserRoleListByUserIds(@Param("userIds") String userIds);

    /**
     * 根据用户id更新用户为超级管理员
     *
     * @param userIds 用户id，支持批量
     * @param isSuper 是否超管
     * @return 更新条数
     */
    int updateSuperUserByUserIds(@Param("userIds") List<Long> userIds, @Param("isSuper") Integer isSuper);
}
