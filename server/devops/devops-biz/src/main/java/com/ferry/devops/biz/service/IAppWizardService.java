package com.ferry.devops.biz.service;

import com.ferry.devops.api.dto.WizardStepDTO;
import com.ferry.devops.api.vo.AppOverviewVO;
import com.ferry.devops.api.vo.WizardProgressVO;

/**
 * 应用接入向导与详情聚合服务
 */
public interface IAppWizardService {

    boolean saveStep(WizardStepDTO body);

    WizardProgressVO progress(Long appId);

    AppOverviewVO overview(Long appId);
}
