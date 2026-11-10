package com.ferry.system.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.system.api.dto.query.UserQuery;
import com.ferry.system.api.entity.User;
import com.ferry.system.api.vo.UserVO;

public class UserWrapper extends BaseEntityWrapper<User, UserVO> {

    public static UserWrapper build() {
        return new UserWrapper();
    }

    @Override
    public UserVO vo(User entity) {
        return BeanUtil.copy(entity, UserVO.class);
    }

    public LambdaQueryWrapper<User> lambda(UserQuery query) {
        LambdaQueryWrapper<User> queryWrapper = Wrappers.<User>lambdaQuery();
        String tenantId = query.getTenantId();
        queryWrapper.eq(StrUtil.isNotBlank(tenantId), User::getTenantId, tenantId);
        String account = query.getAccount();
        queryWrapper.like(StrUtil.isNotBlank(account), User::getAccount, account);
        String nickname = query.getNickname();
        queryWrapper.like(StrUtil.isNotBlank(nickname), User::getNickname, nickname);
        Integer status = query.getStatus();
        queryWrapper.eq(ObjUtil.isNotEmpty(status), User::getStatus, status);
        String deptId = query.getDeptId();
        queryWrapper.like(StrUtil.isNotBlank(deptId), User::getDeptId, deptId);
        queryWrapper.eq(User::getIsSuper, IntConst.ZERO);
        queryWrapper.ne(User::getId, AuthUtil.getUserId());
        queryWrapper.orderByDesc(CollUtil.asList(User::getIsSuper, User::getStatus, User::getCreateTime));
        return queryWrapper;
    }
}
