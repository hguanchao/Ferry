package com.ferry.system.biz.service;

import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.entity.RoleDept;

public interface IRoleDeptService extends IService<RoleDept> {

    /**
     * 角色授权部门
     *
     * @param body 参数体
     * @return 是否成功
     */
    boolean authorizeDept(RoleAuthorize<Long, Long> body);
}
