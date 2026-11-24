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
import com.ferry.devops.api.dto.AppDTO;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.model.JenkinsJob;
import com.ferry.devops.api.vo.AppOptionVO;
import com.ferry.devops.api.vo.AppVO;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
import com.ferry.devops.biz.integration.jenkins.JenkinsJobNames;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.service.IAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 应用档案服务实现。
 */
@Service
@RequiredArgsConstructor
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements IAppService {
    private final JenkinsClient jenkinsClient;

    @Override
    public IPage<AppVO> pageList(AppQuery body) {
        return baseMapper.selectPageList(Page.create(body), body);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(AppDTO body) {
        long count = count(Wrappers.<App>lambdaQuery().eq(App::getCode, body.getCode())
                .ne(ObjUtil.isNotEmpty(body.getId()), App::getId, body.getId()));
        if (count > IntConst.ZERO) {
            throw new ServiceException("应用编码已存在，请更换");
        }
        if (ObjUtil.isNotEmpty(body.getId())) {
            App app = getById(body.getId());
            if (app == null) {
                throw new ServiceException("应用不存在");
            }
            if (!ObjUtil.equals(app.getCode(), body.getCode())) {
                throw new ServiceException("应用编码创建后不可修改");
            }
            if (StrUtil.isBlank(body.getRepoToken())) {
                body.setRepoToken(app.getRepoToken());
            }
        }
        App app = BeanUtil.copy(body, App.class);
        boolean isSuccess = saveOrUpdate(app);
        if (isSuccess) {
            JenkinsJob job = new JenkinsJob();
            job.setName(JenkinsJobNames.appJobName(app.getCode()));
            job.setAppCode(app.getCode().trim());
            job.setScript(PipelineScriptRenderer.render(new PipelineDefinitionDTO()));
            job.setDisabled(true);
            job.setRepoUrl(app.getRepoUrl());
            job.setRepoName(app.getName());
            if (ObjUtil.isNotEmpty(body.getId())) {
                String jobName = job.getName();
                if (!jenkinsClient.jobExists(jobName)) {
                    jenkinsClient.createJob(job);
                }
                jenkinsClient.updateJob(job);
            } else {
                jenkinsClient.createJob(job);
            }
        }
        return isSuccess;
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
        List<App> appList = listByIds(ids);
        boolean isSuccess = removeByIds(ids);
        if (isSuccess) {
            // Job 与应用同生命周期：Jenkins 删除失败即整体回滚，应用保留
            appList.forEach(app -> jenkinsClient.deleteJob(JenkinsJobNames.appJobName(app.getCode())));
        }
        return isSuccess;
    }
}
