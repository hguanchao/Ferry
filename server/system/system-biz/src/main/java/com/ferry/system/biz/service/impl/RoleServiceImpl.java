package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.dto.RoleDTO;
import com.ferry.system.api.dto.query.RoleQuery;
import com.ferry.system.api.entity.Menu;
import com.ferry.system.api.entity.Role;
import com.ferry.system.api.entity.RoleMenu;
import com.ferry.system.api.entity.UserRole;
import com.ferry.system.api.vo.RoleVO;
import com.ferry.system.api.vo.tree.MenuTree;
import com.ferry.system.biz.mapper.RoleMapper;
import com.ferry.system.biz.service.IRoleMenuService;
import com.ferry.system.biz.service.IRoleService;
import com.ferry.system.biz.service.IUserRoleService;
import com.ferry.system.biz.wrapper.RoleWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements IRoleService {
    private final IRoleMenuService roleMenuService;
    private final IUserRoleService userRoleService;

    @Override
    public IPage<RoleVO> pageList(RoleQuery body) {
        RoleWrapper wrapper = RoleWrapper.build();
        IPage<Role> pages = page(Page.create(body), wrapper.lambda(body));
        return wrapper.pageVo(pages);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(RoleDTO body) {
        Role role = BeanUtil.copy(body, Role.class);
        long count = count(Wrappers.<Role>lambdaQuery().eq(Role::getCode, role.getCode())
                .ne(ObjUtil.isNotEmpty(role.getId()), Role::getId, role.getId()));
        if (count > IntConst.ZERO) {
            throw new ServiceException("角色编码[" + role.getCode() + "]已存在");
        }
        return saveOrUpdate(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的角色");
        }
        // 角色还在给用户使用，删掉会让用户凭空失去权限，先拦住
        long userCount = userRoleService.count(Wrappers.<UserRole>lambdaQuery().in(UserRole::getRoleId, ids));
        if (userCount > IntConst.ZERO) {
            throw new ServiceException("角色已分配给用户，请先解除授权");
        }
        boolean isSuccess = removeByIds(ids);
        if (isSuccess) {
            roleMenuService.remove(Wrappers.<RoleMenu>lambdaQuery().in(RoleMenu::getRoleId, ids));
        }
        return isSuccess;
    }

    @Override
    public List<MenuTree> menuList(Long id) {
        List<Menu> menus = roleMenuService.roleMenus(CollUtil.asList(id));
        return BeanUtil.copy(menus, MenuTree.class);
    }

    @Override
    public boolean authorizeMenu(RoleAuthorize<Long, Long> body) {
        return roleMenuService.authorizeMenu(body);
    }
}
