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
import com.ferry.system.api.entity.Role;
import com.ferry.system.api.entity.UserRole;
import com.ferry.system.biz.mapper.UserRoleMapper;
import com.ferry.system.biz.service.IUserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ferry.common.core.constant.SysConst.SUPER_ADMIN_CODE;

@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements IUserRoleService {

    @Override
    public List<Role> roles(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return baseMapper.selectRoleListByRoleIds(StrUtil.join(roleIds));
    }

    @Override
    public List<Role> userRoles(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return baseMapper.selectUserRoleListByUserIds(StrUtil.join(userIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean authorizeRole(RoleAuthorize<Long, Long> body) {
        // 授权会改写用户的超管标记，属于提权操作，只允许超管执行
        if (!AuthUtil.isSuper()) {
            throw new AuthException(HttpStatus.FORBIDDEN);
        }
        List<Long> userIds = body.getAuthorizedIds();
        List<Long> roleIds = body.getAuthorizeIds();
        if (CollUtil.isEmpty(roleIds) || CollUtil.isEmpty(userIds)) {
            throw new ServiceException("请选择要授权的用户或角色");
        }
        remove(Wrappers.<UserRole>lambdaQuery().in(UserRole::getUserId, userIds));
        List<UserRole> userRoleList = new ArrayList<>();
        userIds.forEach(userId -> roleIds.forEach(roleId -> userRoleList.add(new UserRole(userId, roleId))));
        boolean isSuccess = saveBatch(userRoleList);
        if (isSuccess) {
            List<Role> roles = roles(roleIds);
            List<String> roleCodes = roles.stream().map(Role::getCode).toList();
            int isSuper = roleCodes.contains(SUPER_ADMIN_CODE) ? 1 : 0;
            // 授权后按用户重算超管标记，避免状态残留
            baseMapper.updateSuperUserByUserIds(userIds, isSuper);
        }
        return isSuccess;
    }
}
