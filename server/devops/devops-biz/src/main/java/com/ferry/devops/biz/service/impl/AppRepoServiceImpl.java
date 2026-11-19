package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.dto.AppRepoDTO;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.AppRepo;
import com.ferry.devops.api.enums.RepoTypeEnum;
import com.ferry.devops.api.vo.AppRepoVO;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.AppRepoMapper;
import com.ferry.devops.biz.service.IAppRepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用代码源服务实现
 */
@Service
@RequiredArgsConstructor
public class AppRepoServiceImpl extends ServiceImpl<AppRepoMapper, AppRepo> implements IAppRepoService {
    private final AppMapper appMapper;

    @Override
    public List<AppRepoVO> repoList(Long appId) {
        return BeanUtil.copy(baseMapper.selectList(Wrappers.<AppRepo>lambdaQuery()
                .eq(AppRepo::getAppId, appId)), AppRepoVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRepo(AppRepoDTO body) {
        requireApp(body.getAppId());
        checkRepoUrl(body.getPlatform(), body.getRepoUrl());
        if (ObjUtil.isNotEmpty(body.getIsMain()) && body.getIsMain() == IntConst.ONE) {
            AppRepo main = baseMapper.selectOne(Wrappers.<AppRepo>lambdaQuery()
                    .eq(AppRepo::getAppId, body.getAppId()).eq(AppRepo::getIsMain, IntConst.ONE)
                    .ne(ObjUtil.isNotEmpty(body.getId()), AppRepo::getId, body.getId()));
            if (main != null) {
                throw new ServiceException("应用已存在主仓库");
            }
        }
        AppRepo repo = BeanUtil.copy(body, AppRepo.class);
        if (ObjUtil.isEmpty(repo.getId())) {
            return baseMapper.insert(repo) > IntConst.ZERO;
        }
        return baseMapper.updateById(repo) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletedRepo(List<Long> ids) {
        List<AppRepo> repos = baseMapper.selectBatchIds(ids);
        if (repos.stream().anyMatch(r -> ObjUtil.equals(r.getIsMain(), IntConst.ONE))) {
            throw new ServiceException("主仓库不可删除，请先转移");
        }
        return baseMapper.deleteBatchIds(ids) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWizardRepo(Long appId, String data) {
        AppRepo repo = new AppRepo();
        repo.setAppId(appId);
        repo.setPlatform(JsonUtil.getInt(data, "platform"));
        repo.setRepoPath(JsonUtil.getStr(data, "repoPath"));
        repo.setRepoUrl(JsonUtil.getStr(data, "repoUrl"));
        repo.setRepoToken(JsonUtil.getStr(data, "repoToken"));
        repo.setDefaultBranch(JsonUtil.getStr(data, "defaultBranch"));
        repo.setIsMain(IntConst.ONE);
        baseMapper.insert(repo);
        App app = appMapper.selectById(appId);
        if (app != null) {
            app.setSetupStatus(com.ferry.devops.api.enums.SetupStatusEnum.COMPLETE.getCode());
            appMapper.updateById(app);
        }
    }

    @Override
    public boolean hasRepo(Long appId) {
        return baseMapper.selectCount(Wrappers.<AppRepo>lambdaQuery().eq(AppRepo::getAppId, appId)) > IntConst.ZERO;
    }

    private void requireApp(Long appId) {
        if (appMapper.selectById(appId) == null) {
            throw new ServiceException("应用不存在");
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
}
