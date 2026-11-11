package com.ferry.system.biz.service;

import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.entity.RoleDept;

public interface IRoleDeptService extends IService<RoleDept> {

    boolean authorizeDept(RoleAuthorize<Long, Long> body);
}
