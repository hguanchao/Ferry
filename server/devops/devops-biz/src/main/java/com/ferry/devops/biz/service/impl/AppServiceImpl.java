package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.devops.api.dto.AppCoreFlagApplyDTO;
import com.ferry.devops.api.dto.AppCoreFlagApproveDTO;
import com.ferry.devops.api.dto.AppDTO;
import com.ferry.devops.api.dto.AppDependencyDTO;
import com.ferry.devops.api.dto.AppRepoDTO;
import com.ferry.devops.api.dto.WizardStepDTO;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.AppCoreFlagApply;
import com.ferry.devops.api.entity.AppDependency;
import com.ferry.devops.api.entity.AppOwner;
import com.ferry.devops.api.entity.AppRepo;
import com.ferry.devops.api.enums.RepoTypeEnum;
import com.ferry.devops.api.enums.SetupStatusEnum;
import com.ferry.devops.api.vo.AppDependencyVO;
import com.ferry.devops.api.vo.AppDetailVO;
import com.ferry.devops.api.vo.AppOptionVO;
import com.ferry.devops.api.vo.AppOverviewVO;
import com.ferry.devops.api.vo.AppRepoVO;
import com.ferry.devops.api.vo.AppVO;
import com.ferry.devops.api.vo.WizardProgressVO;
import com.ferry.devops.biz.mapper.AppCoreFlagApplyMapper;
import com.ferry.devops.biz.mapper.AppDependencyMapper;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.AppOwnerMapper;
import com.ferry.devops.biz.mapper.AppRepoMapper;
import com.ferry.devops.biz.service.IAppService;
import com.ferry.devops.biz.wrapper.AppWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 应用管理服务实现
 */
@Service
@RequiredArgsConstructor
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements IAppService {
    private final AppRepoMapper repoMapper;
    private final AppOwnerMapper ownerMapper;
    private final AppDependencyMapper dependencyMapper;
    private final AppCoreFlagApplyMapper coreFlagApplyMapper;

    @Override
    public IPage<AppVO> pageList(AppQuery body) {
        AppWrapper wrapper = AppWrapper.build();
        IPage<App> pages = page(Page.create(body), wrapper.lambda(body));
        IPage<AppVO> result = wrapper.pageVo(pages);
        // 负责人名称按已保存的冗余顺序回填
        for (AppVO vo : result.getRecords()) {
            vo.setOwnerNames(ownerNames(vo.getId(), vo.getOwnerIds()));
        }
        return result;
    }

    @Override
    public AppDetailVO detail(Long id) {
        App app = requireApp(id);
        AppDetailVO vo = BeanUtil.copy(app, AppDetailVO.class);
        vo.setRepos(BeanUtil.copy(repoMapper.selectList(Wrappers.<AppRepo>lambdaQuery()
                .eq(AppRepo::getAppId, id)), AppRepoVO.class));
        vo.setOwners(BeanUtil.copy(ownerMapper.selectList(Wrappers.<AppOwner>lambdaQuery()
                .eq(AppOwner::getAppId, id).orderByAsc(AppOwner::getSort)),
                com.ferry.devops.api.vo.AppOwnerVO.class));
        vo.setDependencies(BeanUtil.copy(dependencyMapper.selectList(Wrappers.<AppDependency>lambdaQuery()
                .eq(AppDependency::getAppId, id)), AppDependencyVO.class));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(AppDTO body) {
        checkCode(body.getCode(), body.getId());
        List<Long> owners = CollUtil.toLongList(body.getOwnerIds());
        if (owners.size() < IntConst.ONE || owners.size() > 3) {
            throw new ServiceException("应用负责人需 1-3 人");
        }
        if (ObjUtil.isNotEmpty(body.getId())) {
            App old = requireApp(body.getId());
            if (!ObjUtil.equals(old.getCode(), body.getCode())) {
                throw new ServiceException("应用编码创建后不可修改");
            }
        }
        App app = BeanUtil.copy(body, App.class);
        boolean success = saveOrUpdate(app);
        saveOwners(app.getId(), body.getOwnerIds(), body.getBackupOwnerIds());
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的应用");
        }
        for (Long id : ids) {
            if (baseMapper.countActiveRun(id) > IntConst.ZERO) {
                throw new ServiceException("应用存在进行中的运行，不可删除");
            }
            if (baseMapper.countActiveDeploy(id) > IntConst.ZERO) {
                throw new ServiceException("应用存在进行中的部署，不可删除");
            }
            if (baseMapper.countOpenRelease(id) > IntConst.ZERO) {
                throw new ServiceException("应用存在未关闭的发布单，不可删除");
            }
        }
        return removeByIds(ids);
    }

    @Override
    public List<AppOptionVO> options(String keyword, Integer scope) {
        List<App> apps = list(Wrappers.<App>lambdaQuery()
                .like(StrUtil.isNotBlank(keyword), App::getName, keyword)
                .orderByAsc(App::getName));
        return apps.stream().map(app -> {
            AppOptionVO vo = new AppOptionVO();
            vo.setValue(app.getId());
            vo.setLabel(app.getName());
            vo.setCode(app.getCode());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRepo(AppRepoDTO body) {
        requireApp(body.getAppId());
        checkRepoUrl(body.getPlatform(), body.getRepoUrl());
        if (ObjUtil.isNotEmpty(body.getIsMain()) && body.getIsMain() == IntConst.ONE) {
            AppRepo main = repoMapper.selectOne(Wrappers.<AppRepo>lambdaQuery()
                    .eq(AppRepo::getAppId, body.getAppId()).eq(AppRepo::getIsMain, IntConst.ONE)
                    .ne(ObjUtil.isNotEmpty(body.getId()), AppRepo::getId, body.getId()));
            if (main != null) {
                throw new ServiceException("应用已存在主仓库");
            }
        }
        AppRepo repo = BeanUtil.copy(body, AppRepo.class);
        if (ObjUtil.isEmpty(repo.getId())) {
            return repoMapper.insert(repo) > IntConst.ZERO;
        }
        return repoMapper.updateById(repo) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletedRepo(List<Long> ids) {
        List<AppRepo> repos = repoMapper.selectBatchIds(ids);
        if (repos.stream().anyMatch(r -> ObjUtil.equals(r.getIsMain(), IntConst.ONE))) {
            throw new ServiceException("主仓库不可删除，请先转移");
        }
        return repoMapper.deleteBatchIds(ids) > IntConst.ZERO;
    }

    @Override
    public List<AppRepoVO> repoList(Long appId) {
        return BeanUtil.copy(repoMapper.selectList(Wrappers.<AppRepo>lambdaQuery()
                .eq(AppRepo::getAppId, appId)), AppRepoVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDependency(AppDependencyDTO body) {
        requireApp(body.getAppId());
        AppDependency dependency = BeanUtil.copy(body, AppDependency.class);
        if (ObjUtil.isEmpty(dependency.getId())) {
            return dependencyMapper.insert(dependency) > IntConst.ZERO;
        }
        return dependencyMapper.updateById(dependency) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletedDependency(List<Long> ids) {
        return dependencyMapper.deleteBatchIds(ids) > IntConst.ZERO;
    }

    @Override
    public List<AppDependencyVO> dependencyList(Long appId) {
        return BeanUtil.copy(dependencyMapper.selectList(Wrappers.<AppDependency>lambdaQuery()
                .eq(AppDependency::getAppId, appId)), AppDependencyVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyCoreFlag(AppCoreFlagApplyDTO body) {
        requireApp(body.getAppId());
        AppCoreFlagApply apply = new AppCoreFlagApply();
        apply.setAppId(body.getAppId());
        apply.setIsTargetCore(body.getIsTargetCore());
        apply.setReason(body.getReason());
        apply.setStatus(IntConst.ONE);
        apply.setApplicant(currentUserId());
        return coreFlagApplyMapper.insert(apply) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveCoreFlag(AppCoreFlagApproveDTO body) {
        AppCoreFlagApply apply = coreFlagApplyMapper.selectById(body.getApplyId());
        if (apply == null || !ObjUtil.equals(apply.getStatus(), IntConst.ONE)) {
            throw new ServiceException("申请不存在或已处理");
        }
        apply.setApprover(currentUserId());
        apply.setApproveTime(new java.util.Date());
        apply.setStatus(Boolean.TRUE.equals(body.getPass()) ? 2 : 3);
        coreFlagApplyMapper.updateById(apply);
        if (Boolean.TRUE.equals(body.getPass())) {
            App app = requireApp(apply.getAppId());
            app.setIsCore(apply.getIsTargetCore());
            updateById(app);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWizardStep(WizardStepDTO body) {
        App app = body.getAppId() == null ? new App() : requireApp(body.getAppId());
        if (ObjUtil.equals(body.getStep(), IntConst.ONE)) {
            applyStepOne(app, body.getData());
            if (app.getId() == null) {
                checkCode(app.getCode(), null);
            }
            return saveOrUpdate(app);
        } else if (ObjUtil.equals(body.getStep(), 2)) {
            applyStepTwo(app, body.getData());
            return true;
        }
        return true;
    }

    @Override
    public WizardProgressVO wizardProgress(Long id) {
        App app = requireApp(id);
        WizardProgressVO vo = new WizardProgressVO();
        vo.setAppId(id);
        vo.setSetupStatus(app.getSetupStatus());
        List<Integer> completed = new ArrayList<>();
        completed.add(IntConst.ONE);
        if (repoMapper.selectCount(Wrappers.<AppRepo>lambdaQuery().eq(AppRepo::getAppId, id)) > IntConst.ZERO) {
            completed.add(2);
        }
        vo.setCompletedSteps(completed);
        vo.setSkippedSteps(new ArrayList<>());
        return vo;
    }

    @Override
    public AppOverviewVO overview(Long id) {
        App app = requireApp(id);
        AppOverviewVO vo = new AppOverviewVO();
        vo.setAppId(id);
        vo.setPipelineCount(0);
        vo.setEnvCount(0);
        vo.setRunningCount(0);
        vo.setBlockers(new ArrayList<>());
        vo.setProdDeployType(null);
        return vo;
    }

    @Override
    public Map<String, String> optionMap() {
        Map<String, String> result = new LinkedHashMap<>();
        for (App app : list(Wrappers.<App>lambdaQuery().orderByAsc(App::getName))) {
            result.put(String.valueOf(app.getId()), app.getName());
        }
        return result;
    }

    @Override
    public List<Long> owners(Long appId) {
        return ownerMapper.selectList(Wrappers.<AppOwner>lambdaQuery()
                        .eq(AppOwner::getAppId, appId).eq(AppOwner::getType, IntConst.ONE))
                .stream().map(AppOwner::getUserId).collect(Collectors.toList());
    }

    private App requireApp(Long id) {
        App app = getById(id);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        return app;
    }

    private void checkCode(String code, Long id) {
        long count = count(Wrappers.<App>lambdaQuery()
                .eq(App::getCode, code)
                .ne(ObjUtil.isNotEmpty(id), App::getId, id));
        if (count > IntConst.ZERO) {
            throw new ServiceException("应用编码已存在，请更换");
        }
        if (!code.matches("[a-z0-9-]{2,40}")) {
            throw new ServiceException("应用编码只能包含小写字母、数字和中划线，长度 2-40");
        }
    }

    private void checkRepoUrl(Integer platform, String repoUrl) {
        if (StrUtil.isBlank(repoUrl)) {
            return;
        }
        RepoTypeEnum repoType = RepoTypeEnum.of(platform);
        if (repoType == null) {
            throw new ServiceException("代码托管平台取值不合法");
        }
        String domain = switch (repoType) {
            case GITHUB -> "github.com";
            case GITEE -> "gitee.com";
            default -> "gitlab";
        };
        if (!repoUrl.contains(domain)) {
            throw new ServiceException("仓库地址域名与所选平台不匹配");
        }
    }

    private void saveOwners(Long appId, String ownerIds, String backupIds) {
        ownerMapper.delete(Wrappers.<AppOwner>lambdaQuery().eq(AppOwner::getAppId, appId));
        int sort = 0;
        for (Long userId : CollUtil.toLongList(ownerIds)) {
            AppOwner owner = new AppOwner();
            owner.setAppId(appId);
            owner.setUserId(userId);
            owner.setType(IntConst.ONE);
            owner.setSort(sort++);
            ownerMapper.insert(owner);
        }
        for (Long userId : CollUtil.toLongList(backupIds)) {
            AppOwner owner = new AppOwner();
            owner.setAppId(appId);
            owner.setUserId(userId);
            owner.setType(2);
            owner.setSort(sort++);
            ownerMapper.insert(owner);
        }
    }

    private String ownerNames(Long appId, String ownerIds) {
        List<AppOwner> owners = ownerMapper.selectList(Wrappers.<AppOwner>lambdaQuery()
                .eq(AppOwner::getAppId, appId).eq(AppOwner::getType, IntConst.ONE));
        Map<Long, String> names = new LinkedHashMap<>();
        for (AppOwner owner : owners) {
            names.put(owner.getUserId(), "用户" + owner.getUserId());
        }
        StringBuilder builder = new StringBuilder();
        for (Long id : CollUtil.toLongList(ownerIds)) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(names.getOrDefault(id, ""));
        }
        return builder.toString();
    }

    private void applyStepOne(App app, String data) {
        app.setCode(com.ferry.common.core.utils.JsonUtil.getStr(data, "code"));
        app.setName(com.ferry.common.core.utils.JsonUtil.getStr(data, "name"));
        app.setTeamId(com.ferry.common.core.utils.JsonUtil.getLong(data, "teamId"));
    }

    private void applyStepTwo(App app, String data) {
        AppRepo repo = new AppRepo();
        repo.setAppId(app.getId());
        repo.setPlatform(com.ferry.common.core.utils.JsonUtil.getInt(data, "platform"));
        repo.setRepoPath(com.ferry.common.core.utils.JsonUtil.getStr(data, "repoPath"));
        repo.setRepoUrl(com.ferry.common.core.utils.JsonUtil.getStr(data, "repoUrl"));
        repo.setDefaultBranch(com.ferry.common.core.utils.JsonUtil.getStr(data, "defaultBranch"));
        repo.setIsMain(IntConst.ONE);
        repoMapper.insert(repo);
        app.setSetupStatus(SetupStatusEnum.COMPLETE.getCode());
        updateById(app);
    }

    private Long currentUserId() {
        return com.ferry.common.security.utils.AuthUtil.getUserId();
    }
}
