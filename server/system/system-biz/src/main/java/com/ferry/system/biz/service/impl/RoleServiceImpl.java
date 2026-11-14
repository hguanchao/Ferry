package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.AuthException;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.http.HttpStatus;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.dto.RoleDTO;
import com.ferry.system.api.dto.query.RoleQuery;
import com.ferry.system.api.entity.Dept;
import com.ferry.system.api.entity.Menu;
import com.ferry.system.api.entity.Role;
import com.ferry.system.api.entity.RoleDept;
import com.ferry.system.api.entity.RoleMenu;
import com.ferry.system.api.entity.UserRole;
import com.ferry.system.api.enums.DataScopeEnum;
import com.ferry.system.api.vo.RoleVO;
import com.ferry.system.api.vo.tree.MenuTree;
import com.ferry.system.biz.mapper.RoleMapper;
import com.ferry.system.biz.service.IDeptService;
import com.ferry.system.biz.service.IRoleDeptService;
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
    private final IRoleDeptService roleDeptService;
    private final IUserRoleService userRoleService;
    private final IDeptService deptService;

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
        // 新角色数据范围先置为自定义空授权，落库默认的「全部数据」等于默认放权
        if (ObjUtil.isEmpty(role.getId())) {
            role.setDataScope(DataScopeEnum.CUSTOM.getCode());
        }
        return saveOrUpdate(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的角色");
        }
        // 拦住已分配给用户的角色，删除会让用户凭空失去权限
        long userCount = userRoleService.count(Wrappers.<UserRole>lambdaQuery().in(UserRole::getRoleId, ids));
        if (userCount > IntConst.ZERO) {
            throw new ServiceException("角色已分配给用户，请先解除授权");
        }
        boolean isSuccess = removeByIds(ids);
        if (isSuccess) {
            roleMenuService.remove(Wrappers.<RoleMenu>lambdaQuery().in(RoleMenu::getRoleId, ids));
            // 连带清掉部门授权记录，否则同编码重建角色会捡到上一份数据范围
            roleDeptService.remove(Wrappers.<RoleDept>lambdaQuery().in(RoleDept::getRoleId, ids));
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

    @Override
    public RoleVO dataScope(Long id) {
        Role role = getById(id);
        if (ObjUtil.isEmpty(role)) {
            throw new ServiceException("角色不存在");
        }
        RoleVO vo = BeanUtil.copy(role, RoleVO.class);
        vo.setDataScope(ObjUtil.isEmpty(role.getDataScope()) ? DataScopeEnum.CUSTOM.getCode() : role.getDataScope());
        List<Long> deptIds = roleDeptService.listObjs(Wrappers.<RoleDept>lambdaQuery()
                .select(RoleDept::getDeptId).eq(RoleDept::getRoleId, id));
        vo.setDeptIds(deptIds);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean authorizeDataScope(Long id, Integer dataScope, List<Long> deptIds) {
        // 数据范围等于数据可见边界，与菜单授权一样只允许超管操作
        if (!AuthUtil.isSuper()) {
            throw new AuthException(HttpStatus.FORBIDDEN);
        }
        Role role = getById(id);
        if (ObjUtil.isEmpty(role)) {
            throw new ServiceException("角色不存在");
        }
        DataScopeEnum scope = DataScopeEnum.of(dataScope);
        if (scope == null) {
            throw new ServiceException("数据范围取值不合法");
        }
        RoleAuthorize<Long,Long> body = new RoleAuthorize<>();
        body.setAuthorizedIds(List.of(role.getId()));
        if (scope == DataScopeEnum.CUSTOM) {
            if (CollUtil.isEmpty(deptIds)) {
                // 拒绝空的「自定义」范围，空集合等于什么都看不到
                throw new ServiceException("自定义数据范围至少要选择一个部门");
            }
            body.setAuthorizeIds(deptIds);
        }
        boolean isSuccess = lambdaUpdate().eq(Role::getId, id).set(Role::getDataScope, scope.getCode()).update();
        if (isSuccess) {
            roleDeptService.authorizeDept(body);
        }
        return isSuccess;
    }
}
