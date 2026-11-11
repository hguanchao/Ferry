package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.exception.AuthException;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.http.HttpStatus;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.entity.RoleDept;
import com.ferry.system.api.entity.RoleMenu;
import com.ferry.system.biz.mapper.RoleDeptMapper;
import com.ferry.system.biz.service.IRoleDeptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoleDeptServiceImpl extends ServiceImpl<RoleDeptMapper, RoleDept> implements IRoleDeptService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean authorizeDept(RoleAuthorize<Long, Long> body) {
        if (!AuthUtil.isSuper()) {
            throw new AuthException(HttpStatus.FORBIDDEN);
        }
        List<Long> roleIds = body.getAuthorizedIds();
        List<Long> deptIds = body.getAuthorizeIds();
        if (CollUtil.isEmpty(roleIds)) {
            throw new ServiceException("请选择要授权的角色");
        }
        // 先清空再按新集合重建，因此空菜单是合法输入，表示撤销该角色的全部菜单授权
        remove(Wrappers.<RoleDept>lambdaQuery().in(RoleDept::getRoleId, roleIds));
        if (CollUtil.isEmpty(deptIds)) {
            return true;
        }
        List<RoleDept> roleDeptList = new ArrayList<>();
        deptIds.forEach(deptId -> roleIds.forEach(roleId -> roleDeptList.add(new RoleDept(roleId, deptId))));
        return saveBatch(roleDeptList);
    }
}
