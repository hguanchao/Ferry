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
        checkCode(body.getCode(), body.getId());
        if (ObjUtil.isNotEmpty(body.getId())) {
            App app = getById(body.getId());
            if (app == null) {
                throw new ServiceException("应用不存在");
            }
            if (!ObjUtil.equals(app.getCode(), body.getCode())) {
                throw new ServiceException("应用编码创建后不可修改");
            }
            // 列表/详情不回显 repoToken，编辑表单提交空值时保留已存令牌，防止误清空
            if (StrUtil.isBlank(body.getRepoToken())) {
                body.setRepoToken(app.getRepoToken());
            }
        }
        App app = BeanUtil.copy(body, App.class);
        boolean isSuccess = saveOrUpdate(app);
        if (isSuccess) {
            // Job 与应用同生命周期：Jenkins 失败即整体回滚，应用不落库，用户重试
            JenkinsJob job = new JenkinsJob();
            job.setName(JenkinsJobNames.appJobName(app.getCode()));
            job.setAppCode(app.getCode().trim());
            job.setScript(PipelineScriptRenderer.render(new PipelineDefinitionDTO()));
            job.setDisabled(true);
            job.setRepoUrl(app.getRepoUrl());
            job.setRepoName(app.getName());
            if (ObjUtil.isNotEmpty(body.getId())) {
                // 编辑：先探测 Job 是否存在，不存在则补建，再刷新脚本与 GitHub 项目属性。
                // 避免 Job 被外部删除或从未同步过时 updateJob 直接报"不存在"导致应用保存失败。
                String jobName = job.getName();
                if (!jenkinsClient.jobExists(jobName)) {
                    jenkinsClient.createJob(job);
                }
                jenkinsClient.updateJob(job);
            } else {
                // 新建：初始化空白 Job
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

    @Override
    public Map<String, String> optionMap() {
        Map<String, String> result = new LinkedHashMap<>();
        for (App app : list(Wrappers.<App>lambdaQuery().orderByAsc(App::getName))) {
            result.put(String.valueOf(app.getId()), app.getName());
        }
        return result;
    }

    @Override
    public void checkCode(String code, Long id) {
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

    @Override
    public boolean saveOrUpdateApp(App app) {
        return saveOrUpdate(app);
    }
}
