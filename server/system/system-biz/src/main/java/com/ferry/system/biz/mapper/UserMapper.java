package com.ferry.system.biz.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.system.api.entity.User;
import com.ferry.system.api.vo.tree.MenuTree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据租户id和用户账号查询用户信息
     *
     * <p>登录时还没有令牌，租户拦截器取不到上下文会拼出空租户条件，
     * 这里跳过拦截器，由入参显式限定租户。
     *
     * @param tenantId 租户id
     * @param account  用户账号
     * @return {@link User} 用户信息
     */
    @InterceptorIgnore(tenantLine = "true")
    User selectByTenantIdAndUsername(@Param("tenantId") String tenantId, @Param("account") String account);

    /**
     * 根据用户id获取用户权限菜单列表
     *
     * <p>登录时无租户上下文，同样跳过拦截器，改由入参显式限定租户，
     * 否则超管分支会查出其它租户的菜单。
     *
     * @param id       用户id
     * @param isSuper  是否超管 0：否 1：是
     * @param tenantId 租户id
     * @return List
     */
    @InterceptorIgnore(tenantLine = "true")
    List<MenuTree> selectPermissionsById(@Param("tenantId") String tenantId, @Param("id") Long id, @Param("isSuper") Integer isSuper);
}
