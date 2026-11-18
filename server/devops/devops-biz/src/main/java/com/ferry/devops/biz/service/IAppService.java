package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.AppCoreFlagApplyDTO;
import com.ferry.devops.api.dto.AppCoreFlagApproveDTO;
import com.ferry.devops.api.dto.AppDTO;
import com.ferry.devops.api.dto.AppDependencyDTO;
import com.ferry.devops.api.dto.AppRepoDTO;
import com.ferry.devops.api.dto.WizardStepDTO;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.vo.AppDetailVO;
import com.ferry.devops.api.vo.AppOptionVO;
import com.ferry.devops.api.vo.AppOverviewVO;
import com.ferry.devops.api.vo.AppVO;
import com.ferry.devops.api.vo.WizardProgressVO;

import java.util.List;
import java.util.Map;

/**
 * 应用管理服务
 */
public interface IAppService extends IService<App> {

    IPage<AppVO> pageList(AppQuery body);

    AppDetailVO detail(Long id);

    boolean save(AppDTO body);

    boolean deleted(List<Long> ids);

    List<AppOptionVO> options(String keyword, Integer scope);

    boolean saveRepo(AppRepoDTO body);

    boolean deletedRepo(List<Long> ids);

    List<com.ferry.devops.api.vo.AppRepoVO> repoList(Long appId);

    boolean saveDependency(AppDependencyDTO body);

    boolean deletedDependency(List<Long> ids);

    List<com.ferry.devops.api.vo.AppDependencyVO> dependencyList(Long appId);

    boolean applyCoreFlag(AppCoreFlagApplyDTO body);

    boolean approveCoreFlag(AppCoreFlagApproveDTO body);

    boolean saveWizardStep(WizardStepDTO body);

    WizardProgressVO wizardProgress(Long id);

    AppOverviewVO overview(Long id);

    /**
     * 内部接口：应用id到名称的映射
     */
    Map<String, String> optionMap();

    /**
     * 内部接口：应用负责人用户id
     */
    List<Long> owners(Long appId);
}
