package com.ferry.system.biz.feign;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.NumUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.security.model.AuthPerm;
import com.ferry.common.security.model.AuthUser;
import com.ferry.system.api.entity.Role;
import com.ferry.system.api.entity.RoleDept;
import com.ferry.system.api.entity.User;
import com.ferry.system.api.enums.DataScopeEnum;
import com.ferry.system.api.feign.ISystemFeign;
import com.ferry.system.api.vo.tree.MenuTree;
import com.ferry.system.biz.service.IDeptService;
import com.ferry.system.biz.service.IRoleDeptService;
import com.ferry.system.biz.service.IUserRoleService;
import com.ferry.system.biz.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统服务契约的服务端实现
 *
 * <p>/internal 前缀必须声明在本类上：接口侧的 {@code @FeignClient} 不允许标注
 * {@code @RequestMapping}，其 path 属性只参与客户端 URL 拼接，不会注册成服务端映射。
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class SystemFeign implements ISystemFeign {
    private final IUserService userService;
    private final IDeptService deptService;
    private final IUserRoleService userRoleService;
    private final IRoleDeptService roleDeptService;

    @Override
    public AuthUser userAuth(String tenantId, String account) {
        User user = userService.auth(tenantId, account);
        if (user == null) {
            return null;
        }
        AuthUser authUser = BeanUtil.copy(user, AuthUser.class);
        List<Role> userRoles = userRoleService.userRoles(CollUtil.asList(user.getId()));
        authUser.setRoleId(userRoles.stream().map(item -> StrUtil.toStr(item.getId())).collect(Collectors.joining(",")));
        List<MenuTree> permissions = userService.permissions(user.getTenantId(), user.getId(), user.getIsSuper());
        authUser.setPermissions(BeanUtil.copy(permissions, AuthPerm.class));
        fillDataScope(authUser, NumUtil.toLong(user.getDeptId()), userRoles);
        return authUser;
    }

    /**
     * 计算数据可见范围
     *
     * <p>登录时算一次随会话缓存，SQL 拦截器直接读快照，不在每次查询时回表。
     * 多角色取并集，任一角色可看全部即为全部。
     *
     * @param authUser 认证用户
     * @param deptId   部门ID
     * @param roles    用户角色
     */
    private void fillDataScope(AuthUser authUser, Long deptId, List<Role> roles) {
        Set<Long> deptIds = new HashSet<>();
        Set<Long> customDeptIds = new HashSet<>();
        boolean self = false;
        for (Role role : roles) {
            DataScopeEnum scope = DataScopeEnum.of(role.getDataScope());
            if (scope == null) {
                continue;
            }
            switch (scope) {
                // 可看全部时其余角色不必再算，直接定稿返回
                case ALL -> {
                    authUser.setDataAll(true);
                    authUser.setDataSelf(false);
                    authUser.setDataDeptIds(Set.of());
                    return;
                }
                case CUSTOM -> customDeptIds.addAll(roleDeptService.listObjs(Wrappers.<RoleDept>lambdaQuery()
                        .select(RoleDept::getDeptId).eq(RoleDept::getRoleId, role.getId())));
                case DEPT -> Optional.ofNullable(deptId).ifPresent(deptIds::add);
                case DEPT_AND_CHILD -> Optional.ofNullable(deptId)
                        .ifPresent(id -> deptIds.addAll(deptService.selfAndChildIds(List.of(id))));
                case SELF -> self = true;
            }
        }
        if (!customDeptIds.isEmpty()) {
            deptIds.addAll(deptService.selfAndChildIds(List.copyOf(customDeptIds)));
        }
        authUser.setDataAll(false);
        authUser.setDataSelf(self);
        authUser.setDataDeptIds(deptIds);
    }
}
