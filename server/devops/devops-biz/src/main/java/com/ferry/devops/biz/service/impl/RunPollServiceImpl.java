package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.entity.RunTask;
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.biz.config.DevopsIntegrationProperties;
import com.ferry.devops.biz.integration.jenkins.JenkinsBuildStatus;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
import com.ferry.devops.biz.integration.jenkins.JenkinsDispatchException;
import com.ferry.devops.biz.integration.jenkins.JenkinsStageStatus;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.mapper.RunTaskMapper;
import com.ferry.devops.biz.service.IRunPollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 轮询 Jenkins 构建状态并同步运行/阶段：状态同步的兜底对账通道（主通道为脚本内 webhook 回调）。
 *
 * <p>按状态分频：活动运行（排队中/运行中）60 秒级、结果待确认 300 秒级；
 * 均只处理最近 24 小时内的运行。构建号未分配时先通过队列项解析。
 * 阶段状态来自 Pipeline Stage View 的 wfapi，插件不可用时降级为构建级状态；
 * 回调已写入的终态阶段在此保持幂等（终态不覆盖）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunPollServiceImpl implements IRunPollService {

    private static final int POLL_LIMIT = 50;
    private static final long LOOKBACK_MS = 24L * 60 * 60 * 1000;
    private static final int TASK_STATUS_SUCCESS = 3;
    private static final int TASK_STATUS_FAILURE = 4;
    private static final int TASK_STATUS_SKIPPED = 6;
    private static final int TASK_STATUS_CANCELLED = 9;

    private final RunMapper runMapper;
    private final RunStageMapper stageMapper;
    private final RunTaskMapper taskMapper;
    private final AppMapper appMapper;
    private final JenkinsClient jenkinsClient;
    private final DevopsIntegrationProperties properties;

    @Override
    public void pollActiveRuns() {
        pollRuns(RunStatusEnum.QUEUED, RunStatusEnum.RUNNING);
    }

    @Override
    public void pollUnknownRuns() {
        pollRuns(RunStatusEnum.UNKNOWN);
    }

    /**
     * 按状态集合轮询运行并同步 Jenkins 状态。
     *
     * @param statuses 本次要轮询的运行状态
     */
    private void pollRuns(RunStatusEnum... statuses) {
        if (properties.getJenkins() == null || StrUtil.isBlank(properties.getJenkins().getBaseUrl())) {
            return;
        }
        Date since = new Date(System.currentTimeMillis() - LOOKBACK_MS);
        List<Run> runs = runMapper.selectList(Wrappers.<Run>lambdaQuery()
                .in(Run::getStatus, Arrays.stream(statuses).map(RunStatusEnum::getCode).toList())
                .gt(Run::getCreateTime, since)
                .orderByAsc(Run::getId)
                .last("LIMIT " + POLL_LIMIT));
        for (Run run : runs) {
            try {
                pollRun(run);
            } catch (RuntimeException e) {
                log.error("poll run failed: runId={}", run.getId(), e);
            }
        }
    }

    private void pollRun(Run run) {
        App app = appMapper.selectById(run.getAppId());
        if (app == null || StrUtil.isBlank(app.getCode())) {
            return;
        }
        // Job 名取运行记录的实际值；旧数据（旧命名协议）回退应用编码 Job
        String jobName = StrUtil.isNotBlank(run.getJobName()) ? run.getJobName() : app.getCode().trim();
        Long buildNo = run.getJenkinsBuildNo();
        if (buildNo == null) {
            if (run.getJenkinsQueueId() == null) {
                return;
            }
            buildNo = jenkinsClient.resolveQueueBuild(jobName, run.getJenkinsQueueId());
            if (buildNo == null) {
                return;
            }
            run.setJenkinsBuildNo(buildNo);
            runMapper.updateById(run);
        }
        JenkinsBuildStatus status;
        try {
            status = jenkinsClient.buildStatus(jobName, buildNo);
        } catch (JenkinsDispatchException e) {
            log.debug("poll jenkins failed: runId={}, buildNo={}, uncertain={}, reason={}",
                    run.getId(), buildNo, e.isUncertain(), e.getMessage());
            return;
        }
        applyStatus(run, status);
    }

    private void applyStatus(Run run, JenkinsBuildStatus status) {
        List<RunStage> stages = stageMapper.selectByRun(run.getId());
        if (!status.stages().isEmpty()) {
            applyStageStatuses(stages, status.stages());
        }
        if (status.building()) {
            boolean changed = false;
            if (ObjUtil.equals(run.getStatus(), RunStatusEnum.QUEUED.getCode())
                    || ObjUtil.equals(run.getStatus(), RunStatusEnum.UNKNOWN.getCode())) {
                run.setStatus(RunStatusEnum.RUNNING.getCode());
                changed = true;
            }
            if (run.getStartTime() == null && status.timestamp() != null && status.timestamp() > 0) {
                run.setStartTime(new Date(status.timestamp()));
                changed = true;
            }
            if (changed) {
                runMapper.updateById(run);
                log.info("run became running: runId={}, buildNo={}", run.getId(), status.buildNo());
            }
            return;
        }
        String result = status.result();
        if (StrUtil.isBlank(result)) {
            return;
        }
        RunStatusEnum terminal = switch (result.toUpperCase()) {
            case "SUCCESS" -> RunStatusEnum.SUCCESS;
            case "ABORTED", "NOT_BUILT" -> RunStatusEnum.CANCELLED;
            case "FAILURE", "UNSTABLE" -> RunStatusEnum.FAILURE;
            default -> null;
        };
        if (terminal == null) {
            return;
        }
        Date endTime = terminalTime(status);
        if (run.getStartTime() == null && status.timestamp() != null && status.timestamp() > 0) {
            run.setStartTime(new Date(status.timestamp()));
        }
        run.setStatus(terminal.getCode());
        run.setEndTime(endTime);
        if (run.getStartTime() != null && endTime != null) {
            run.setDuration(endTime.getTime() - run.getStartTime().getTime());
        }
        if (terminal == RunStatusEnum.SUCCESS) {
            if (status.stages().isEmpty()) {
                // Stage View 不可用：构建成功则视为全部 CI 阶段成功
                markAllCiSuccess(stages, run, endTime);
            }
            markNotStartedSkipped(stages);
        } else {
            // 只跳过未下发的部署/发布阶段；CI 阶段尽量保留 wfapi 结果
            markNonCiSkipped(stages);
        }
        runMapper.updateById(run);
        log.info("run status synced from Jenkins: runId={}, buildNo={}, status={}, result={}",
                run.getId(), status.buildNo(), terminal, result);
    }

    private void applyStageStatuses(List<RunStage> stages, List<JenkinsStageStatus> jenkinsStages) {
        List<RunStage> ciStages = stages.stream()
                .filter(stage -> stage.getStageType() != null && stage.getStageType() <= 4)
                .sorted(Comparator.comparingInt(stage -> stage.getSeq() == null ? Integer.MAX_VALUE : stage.getSeq()))
                .toList();
        List<JenkinsStageStatus> relevant = jenkinsStages.stream()
                .filter(stage -> StrUtil.isNotBlank(stage.name()))
                .filter(stage -> !stage.name().toLowerCase().contains("checkout"))
                .toList();
        Map<String, JenkinsStageStatus> byName = relevant.stream()
                .collect(Collectors.toMap(JenkinsStageStatus::name, stage -> stage, (left, right) -> right));
        for (int i = 0; i < ciStages.size(); i++) {
            RunStage stage = ciStages.get(i);
            if (ObjUtil.equals(stage.getStatus(), StageStatusEnum.REUSED.getCode())) {
                continue;
            }
            JenkinsStageStatus jenkinsStage = byName.get(stage.getStageName());
            if (jenkinsStage == null && i < relevant.size()) {
                jenkinsStage = relevant.get(i);
            }
            if (jenkinsStage == null) {
                continue;
            }
            StageStatusEnum stageStatus = mapStageStatus(jenkinsStage.status());
            if (stageStatus == null) {
                continue;
            }
            stage.setStatus(stageStatus.getCode());
            if (jenkinsStage.startTimeMillis() != null && jenkinsStage.startTimeMillis() > 0) {
                stage.setStartTime(new Date(jenkinsStage.startTimeMillis()));
                if (stageStatus == StageStatusEnum.RUNNING) {
                    stage.setEndTime(null);
                    stage.setDuration(null);
                } else if (jenkinsStage.durationMillis() != null && jenkinsStage.durationMillis() > 0) {
                    stage.setEndTime(new Date(jenkinsStage.startTimeMillis() + jenkinsStage.durationMillis()));
                    stage.setDuration(jenkinsStage.durationMillis());
                }
            }
            stageMapper.updateById(stage);
            syncTaskStatus(stage, stageStatus);
        }
    }

    /**
     * 任务状态暂按阶段状态镜像：Jenkins 阶段级结果无法精确到任务，先保证页面状态一致。
     */
    private void syncTaskStatus(RunStage stage, StageStatusEnum stageStatus) {
        Integer taskStatus = switch (stageStatus) {
            case SUCCESS -> TASK_STATUS_SUCCESS;
            case FAILURE -> TASK_STATUS_FAILURE;
            case SKIPPED -> TASK_STATUS_SKIPPED;
            case CANCELLED -> TASK_STATUS_CANCELLED;
            default -> null;
        };
        if (taskStatus == null) {
            return;
        }
        for (RunTask task : taskMapper.selectByStage(stage.getId())) {
            if (!ObjUtil.equals(task.getStatus(), taskStatus)) {
                task.setStatus(taskStatus);
                taskMapper.updateById(task);
            }
        }
    }

    private StageStatusEnum mapStageStatus(String status) {
        if (StrUtil.isBlank(status)) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> StageStatusEnum.SUCCESS;
            case "FAILED", "FAILURE", "UNSTABLE" -> StageStatusEnum.FAILURE;
            case "IN_PROGRESS", "PAUSED_PENDING_INPUT" -> StageStatusEnum.RUNNING;
            case "ABORTED" -> StageStatusEnum.CANCELLED;
            case "NOT_EXECUTED" -> StageStatusEnum.SKIPPED;
            default -> null;
        };
    }

    private void markAllCiSuccess(List<RunStage> stages, Run run, Date endTime) {
        Date start = run.getStartTime() != null ? run.getStartTime() : endTime;
        for (RunStage stage : stages) {
            if (stage.getStageType() == null || stage.getStageType() > 4
                    || ObjUtil.equals(stage.getStatus(), StageStatusEnum.REUSED.getCode())) {
                continue;
            }
            stage.setStatus(StageStatusEnum.SUCCESS.getCode());
            if (stage.getStartTime() == null && start != null) {
                stage.setStartTime(start);
            }
            stage.setEndTime(endTime);
            if (stage.getStartTime() != null && endTime != null) {
                stage.setDuration(endTime.getTime() - stage.getStartTime().getTime());
            }
            stageMapper.updateById(stage);
            syncTaskStatus(stage, StageStatusEnum.SUCCESS);
        }
    }

    private void markNotStartedSkipped(List<RunStage> stages) {
        for (RunStage stage : stages) {
            if (ObjUtil.equals(stage.getStatus(), StageStatusEnum.NOT_STARTED.getCode())) {
                stage.setStatus(StageStatusEnum.SKIPPED.getCode());
                stageMapper.updateById(stage);
                syncTaskStatus(stage, StageStatusEnum.SKIPPED);
            }
        }
    }

    private void markNonCiSkipped(List<RunStage> stages) {
        for (RunStage stage : stages) {
            if (stage.getStageType() != null && stage.getStageType() > 4
                    && ObjUtil.equals(stage.getStatus(), StageStatusEnum.NOT_STARTED.getCode())) {
                stage.setStatus(StageStatusEnum.SKIPPED.getCode());
                stageMapper.updateById(stage);
                syncTaskStatus(stage, StageStatusEnum.SKIPPED);
            }
        }
    }

    private Date terminalTime(JenkinsBuildStatus status) {
        if (status.timestamp() != null && status.timestamp() > 0
                && status.duration() != null && status.duration() > 0) {
            return new Date(status.timestamp() + status.duration());
        }
        return new Date();
    }
}
