package com.ferry.devops.biz.service;

import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.GateBaselineResetDTO;
import com.ferry.devops.api.entity.GateBaseline;
import com.ferry.devops.api.vo.BaselineVO;

/**
 * 存量基线服务。
 */
public interface IGateBaselineService extends IService<GateBaseline> {

    BaselineVO detail(Long appId);

    boolean reset(GateBaselineResetDTO body);

    boolean approve(Long applyId, Boolean pass, String opinion);

    /** 判定问题是否为新增：指纹不在当前生效基线则返回 true */
    boolean isNewIssue(Long appId, String fingerprint);
}
