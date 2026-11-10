package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.constant.StrConst;
import com.ferry.common.core.constant.SysConst;
import com.ferry.common.core.enums.GenderEnum;
import com.ferry.common.core.exception.AuthException;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.http.HttpStatus;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.CryptoUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.dto.UserDTO;
import com.ferry.system.api.dto.query.UserQuery;
import com.ferry.system.api.entity.User;
import com.ferry.system.api.entity.UserRole;
import com.ferry.system.api.vo.UserVO;
import com.ferry.system.api.vo.tree.MenuTree;
import com.ferry.system.biz.mapper.UserMapper;
import com.ferry.system.biz.service.IUserRoleService;
import com.ferry.system.biz.service.IUserService;
import com.ferry.system.biz.wrapper.UserWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    private final IUserRoleService userRoleService;

    @Override
    public User auth(String tenantId, String account) {
        return baseMapper.selectByTenantIdAndUsername(tenantId, account);
    }

    @Override
    public IPage<UserVO> pageList(UserQuery body) {
        UserWrapper wrapper = UserWrapper.build();
        IPage<UserVO> pages = wrapper.pageVo(page(Page.create(body), wrapper.lambda(body)));
        if (CollUtil.isNotEmpty(pages.getRecords())) {
            List<Long> userIds = pages.getRecords().stream().map(UserVO::getId).collect(Collectors.toList());
            Map<Long, List<Long>> userRoleMap = userRoleService.list(Wrappers.<UserRole>lambdaQuery().in(UserRole::getUserId, userIds))
                    .stream().collect(Collectors.groupingBy(UserRole::getUserId, Collectors.mapping(UserRole::getRoleId, Collectors.toList())));
            for (UserVO record : pages.getRecords()) {
                List<Long> roleIds = userRoleMap.get(record.getId());
                if (CollUtil.isNotEmpty(roleIds)) {
                    record.setRoleId(StrUtil.join(roleIds));
                }
            }
        }
        return pages;
    }

    @Override
    public UserVO detail(Long id) {
        User user = getById(id);
        if (ObjUtil.isEmpty(user)) {
            throw new ServiceException("用户不存在");
        }
        UserVO vo = BeanUtil.copy(user, UserVO.class);
        List<Long> roleIds = userRoleService.list(Wrappers.<UserRole>lambdaQuery()
                .eq(UserRole::getUserId, id), UserRole::getRoleId);
        vo.setRoleId(StrUtil.join(roleIds, StrConst.COMMA));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(UserDTO body) {
        User user = BeanUtil.copy(body, User.class);
        long count = count(Wrappers.<User>lambdaQuery().eq(User::getAccount, user.getAccount())
                .ne(ObjUtil.isNotEmpty(user.getId()), User::getId, user.getId()));
        if (count > IntConst.ZERO) {
            throw new ServiceException("账号[" + user.getAccount() + "]已存在");
        }
        if (ObjUtil.isEmpty(user.getId())) {
            user.setAvatar(GenderEnum.getAvatar(user.getGender()));
            user.setPassword(CryptoUtil.encodePassword(SysConst.DEFAULT_PASSWORD));
        } else if (StrUtil.isNotBlank(body.getPassword())) {
            user.setPassword(CryptoUtil.encodePassword(body.getPassword()));
        } else {
            user.setPassword(null);
        }
        boolean isCreate = ObjUtil.isEmpty(body.getId());
        boolean isSuccess = saveOrUpdate(user);
        if (isSuccess) {
            String roleId = body.getRoleId();
            if (StrUtil.isNotBlank(roleId)) {
                // 用户授权角色
                RoleAuthorize<Long, Long> roleAuthorize = new RoleAuthorize<>(
                        CollUtil.toLongList(roleId), CollUtil.asList(user.getId())
                );
                userRoleService.authorizeRole(roleAuthorize);
            }
            // 改密或禁用之后旧令牌必须失效，否则被停用的账号仍能凭既有会话继续访问
            boolean revokeRequired = StrUtil.isNotBlank(body.getPassword()) || SysConst.IS_DISABLE.equals(user.getStatus());
            if (!isCreate && revokeRequired) {
                AuthUtil.revokeAll(user.getId());
            }
        }
        return isSuccess;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long id) {
        // 重置密码等同于接管账号，仅超管可执行
        if (!AuthUtil.isSuper()) {
            throw new AuthException(HttpStatus.FORBIDDEN);
        }
        if (ObjUtil.isEmpty(getById(id))) {
            throw new ServiceException("用户不存在");
        }
        String password = CryptoUtil.encodePassword(SysConst.DEFAULT_PASSWORD);
        boolean isSuccess = update(Wrappers.<User>lambdaUpdate().set(User::getPassword, password).eq(User::getId, id));
        if (isSuccess) {
            // 重置密码等同于接管账号，旧会话不能继续有效
            AuthUtil.revokeAll(id);
        }
        return isSuccess;
    }

    @Override
    public List<MenuTree> permissions(String tenantId, Long id, Integer isSuper) {
        return baseMapper.selectPermissionsById(tenantId, id, isSuper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的用户");
        }
        boolean isSuccess = removeByIds(ids);
        if (isSuccess) {
            // 用户已删除，令牌若继续有效，被删账号仍可访问系统
            ids.forEach(AuthUtil::revokeAll);
            userRoleService.remove(Wrappers.<UserRole>lambdaQuery().in(UserRole::getUserId, ids));
        }
        return isSuccess;
    }

    @Override
    public boolean authorizeRole(RoleAuthorize<Long, Long> body) {
        return userRoleService.authorizeRole(body);
    }
}
