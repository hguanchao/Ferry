package com.ferry.devops.biz.service;

import com.ferry.devops.api.dto.AppCoreFlagApplyDTO;
import com.ferry.devops.api.dto.AppCoreFlagApproveDTO;
import com.ferry.devops.api.entity.AppCoreFlagApply;

/**
 * 核心链路变更服务
 */
public interface IAppCoreFlagService {

    boolean apply(AppCoreFlagApplyDTO body);

    boolean approve(AppCoreFlagApproveDTO body);

    AppCoreFlagApply get(Long applyId);
}
