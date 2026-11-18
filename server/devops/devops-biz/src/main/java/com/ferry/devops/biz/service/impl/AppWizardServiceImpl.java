package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.devops.api.dto.WizardStepDTO;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.vo.AppOverviewVO;
import com.ferry.devops.api.vo.WizardProgressVO;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.service.IAppRepoService;
import com.ferry.devops.biz.service.IAppService;
import com.ferry.devops.biz.service.IAppWizardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用接入向导与详情聚合服务实现
 */
@Service
@RequiredArgsConstructor
public class AppWizardServiceImpl implements IAppWizardService {
    private final AppMapper appMapper;
    private final IAppService appService;
    private final IAppRepoService repoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveStep(WizardStepDTO body) {
        App app = body.getAppId() == null ? new App() : requireApp(body.getAppId());
        if (ObjUtil.equals(body.getStep(), IntConst.ONE)) {
            applyStepOne(app, body.getData());
            if (app.getId() == null) {
                appService.checkCode(app.getCode(), null);
            }
            return appService.saveOrUpdateApp(app);
        } else if (ObjUtil.equals(body.getStep(), 2)) {
            repoService.saveWizardRepo(app.getId(), body.getData());
            return true;
        }
        return true;
    }

    @Override
    public WizardProgressVO progress(Long appId) {
        App app = requireApp(appId);
        WizardProgressVO vo = new WizardProgressVO();
        vo.setAppId(appId);
        vo.setSetupStatus(app.getSetupStatus());
        List<Integer> completed = new ArrayList<>();
        completed.add(IntConst.ONE);
        if (repoService.hasRepo(appId)) {
            completed.add(2);
        }
        vo.setCompletedSteps(completed);
        vo.setSkippedSteps(new ArrayList<>());
        return vo;
    }

    @Override
    public AppOverviewVO overview(Long appId) {
        App app = requireApp(appId);
        AppOverviewVO vo = new AppOverviewVO();
        vo.setAppId(appId);
        vo.setPipelineCount(0);
        vo.setEnvCount(0);
        vo.setRunningCount(0);
        vo.setBlockers(new ArrayList<>());
        vo.setProdDeployType(null);
        return vo;
    }

    private App requireApp(Long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        return app;
    }

    private void applyStepOne(App app, String data) {
        app.setCode(JsonUtil.getStr(data, "code"));
        app.setName(JsonUtil.getStr(data, "name"));
        app.setTeamId(JsonUtil.getLong(data, "teamId"));
    }
}
