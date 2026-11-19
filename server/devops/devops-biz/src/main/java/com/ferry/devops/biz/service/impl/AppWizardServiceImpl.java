package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.api.dto.WizardStepDTO;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.vo.AppOverviewVO;
import com.ferry.devops.api.vo.WizardProgressVO;
import com.ferry.devops.biz.mapper.AppMapper;
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
            // TODO(dev_app-schema): dev_app_repo 已删除，仓库信息改为内联写入 dev_app。
            applyStepTwo(app, body.getData());
            return appService.saveOrUpdateApp(app);
        }
        return true;
    }

    @Override
    public WizardProgressVO progress(Long appId) {
        App app = requireApp(appId);
        WizardProgressVO vo = new WizardProgressVO();
        vo.setAppId(appId);
        vo.setStatus(app.getStatus());
        List<Integer> completed = new ArrayList<>();
        completed.add(IntConst.ONE);
        // TODO(dev_app-schema): dev_app_repo 已删除，改为按应用内联仓库地址判断是否已完善。
        if (StrUtil.isNotBlank(app.getRepoUrl())) {
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
        app.setDeptId(JsonUtil.getLong(data, "deptId"));
    }

    /** 接入向导第二步：仓库信息内联写入 dev_app。 */
    private void applyStepTwo(App app, String data) {
        app.setRepoType(JsonUtil.getInt(data, "repoType"));
        app.setRepoBranch(JsonUtil.getStr(data, "repoBranch"));
        app.setRepoUrl(JsonUtil.getStr(data, "repoUrl"));
        app.setRepoToken(JsonUtil.getStr(data, "repoToken"));
        app.setStatus(3);
    }
}
