package com.ferry.devops.biz.service.impl;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.enums.PipelineStatusEnum;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
import com.ferry.devops.biz.integration.jenkins.JenkinsDispatchException;
import com.ferry.devops.biz.integration.jenkins.JenkinsJobNames;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.service.IPipelineStageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 流水线 Job 同步编排：渲染脚本 → 校验/同步 Jenkins Job → 按 {@link PipelineStatusEnum} 状态机更新流水线状态。
 *
 * <p>渲染与配置页预览共用 {@link PipelineScriptRenderer#render}，保证“预览即所得”。
 * 数据库与 Jenkins 不宣称原子事务：同步失败时定义已落库，落同步失败状态并返回失败结果，
 * 可通过 {@code /pipeline/sync} 重试；失败详情仅记日志（脱敏），不落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineJobSyncService {

    private final PipelineMapper pipelineMapper;
    private final AppMapper appMapper;
    private final IPipelineStageService pipelineStageService;
    private final RunMapper runMapper;
    private final JenkinsClient jenkinsClient;

    /**
     * 同步结果。
     *
     * @param synced  Jenkins 是否同步成功
     * @param status  同步后的流水线状态（PipelineStatusEnum code）
     * @param message 失败原因（已脱敏）；成功为 null
     */
    public record SyncOutcome(boolean synced, Integer status, String message) {
    }

    /**
     * 同步一条流水线的 Jenkins Job（创建或更新），并按状态机规则更新流水线状态。
     *
     * <p>active-run 保护：存在排队/运行/结果未确认的构建时，阻止覆盖可执行脚本（避免排队构建串版本）；
     * 空定义同步（清理为空脚本+禁用）与停用同步不受此限制。
     *
     * @param pipelineId 流水线 id
     * @return 同步结果（含同步后状态）
     * @throws ServiceException 流水线/应用缺失、编码不合法等前置条件不满足
     */
    public SyncOutcome sync(Long pipelineId) {
        Pipeline pipeline = pipelineMapper.selectById(pipelineId);
        if (pipeline == null) {
            throw new ServiceException("流水线不存在，无法同步 Jenkins Job");
        }
        App app = appMapper.selectById(pipeline.getAppId());
        if (app == null || StrUtil.isBlank(app.getCode())) {
            throw new ServiceException("应用不存在或未登记编码，无法同步 Jenkins Job");
        }
        String appCode = app.getCode().trim();
        String jobName = JenkinsJobNames.appJobName(appCode);
        PipelineDefinitionDTO definition = pipelineStageService.loadDefinition(
                pipelineId, pipeline.getVersion() == null ? 0 : pipeline.getVersion());
        boolean emptyDefinition = definition.getStages() == null || definition.getStages().isEmpty();
        String script = PipelineScriptRenderer.render(definition);
        Integer currentStatus = pipeline.getStatus();
        try {
            if (!emptyDefinition && runMapper.countActiveByPipeline(pipelineId) > 0) {
                throw new ServiceException("存在排队/运行/结果未确认的构建，暂不能覆盖可执行脚本，请稍后重试同步");
            }
            boolean disabled = emptyDefinition || PipelineStatusEnum.DISABLED.getCode().equals(currentStatus);
            // 两段式：createJob 保证 Job 存在（已存在幂等跳过），updateJob 更新脚本与属性
            jenkinsClient.createJob(jobName, appCode, script, disabled,
                    app.getRepoUrl(), app.getName());
            jenkinsClient.updateJob(jobName, appCode, script, disabled,
                    app.getRepoUrl(), app.getName());
        } catch (Exception e) {
            // 脱敏日志：仅 Job 名与失败原因文本，不输出 Jenkins 地址、凭据与响应体
            log.warn("Pipeline job sync failed: pipelineId={}, jobName={}, reason={}", pipelineId, jobName, e.getMessage());
            Integer failedStatus = PipelineStatusEnum.DISABLED.getCode().equals(currentStatus)
                    ? PipelineStatusEnum.DISABLED.getCode()
                    : PipelineStatusEnum.SYNC_FAILED.getCode();
            updateStatus(pipelineId, failedStatus);
            return new SyncOutcome(false, failedStatus, friendlyMessage(e));
        }
        Integer successStatus = emptyDefinition
                ? PipelineStatusEnum.PENDING_CONFIG.getCode()
                : (PipelineStatusEnum.DISABLED.getCode().equals(currentStatus)
                ? PipelineStatusEnum.DISABLED.getCode()
                : PipelineStatusEnum.ENABLED.getCode());
        updateStatus(pipelineId, successStatus);
        return new SyncOutcome(true, successStatus, null);
    }

    /**
     * 重试同步失败原因：不确定类失败（连接超时/中断）统一为通用文案，避免日志之外的渠道暴露内部信息。
     */
    private String friendlyMessage(Exception e) {
        if (e instanceof JenkinsDispatchException dispatch && dispatch.isUncertain()) {
            return "Jenkins 连接异常，结果待确认";
        }
        return e.getMessage();
    }

    private void updateStatus(Long pipelineId, Integer status) {
        Pipeline patch = new Pipeline();
        patch.setId(pipelineId);
        patch.setStatus(status);
        pipelineMapper.updateById(patch);
    }
}
