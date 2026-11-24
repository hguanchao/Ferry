package com.ferry.devops.biz.service.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.entity.RunStageTask;
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.biz.config.DevopsProperties;
import com.ferry.devops.biz.integration.jenkins.JenkinsBuildStatus;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
import com.ferry.devops.biz.integration.jenkins.JenkinsDispatchException;
import com.ferry.devops.biz.integration.jenkins.JenkinsStageStatus;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.mapper.RunStageTaskMapper;
import com.ferry.devops.biz.service.IRunPollService;
import com.ferry.devops.biz.service.IRunStageService;
import com.ferry.devops.biz.utils.StageResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 轮询 Jenkins 构建状态并同步运行/阶段：状态同步的唯一通道。
 *
 * <p>单测/静态扫描阶段执行成功后先进入待门禁，由 RunStageGateEvaluator 拉取报告裁决；
 * 轮询不会直接把这类阶段置成功。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunPollServiceImpl implements IRunPollService {

    private static final int POLL_LIMIT = 50;
    private static final long LOOKBACK_MS = 24L * 60 * 60 * 1000;
    private static final int CI_STAGE_TYPE_MAX = 4;
    private static final int TASK_STATUS_SUCCESS = 3;
    private static final int TASK_STATUS_FAILURE = 4;
    private static final int TASK_STATUS_SKIPPED = 6;
    private static final int TASK_STATUS_CANCELLED = 9;

    private final RunMapper runMapper;
    private final RunStageMapper stageMapper;
    private final RunStageTaskMapper taskMapper;
    private final AppMapper appMapper;
    private final JenkinsClient jenkinsClient;
    private final IRunStageService runStageService;
    private final DevopsProperties properties;

    @Override
    public void pollActiveRuns() {
        pollRuns(RunStatusEnum.QUEUED, RunStatusEnum.RUNNING);
    }

    @Override
    public void pollUnknownRuns() {
        pollRuns(RunStatusEnum.UNKNOWN);
    }

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
        String jobName = StrUtil.isNotBlank(run.getJenkinsJobId())
                ? run.getJenkinsJobId() : app.getCode().trim();
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
            if (ObjUtil.equals(run.getStatus(), RunStatusEnum.QUEUED.getCode())
                    || ObjUtil.equals(run.getStatus(), RunStatusEnum.UNKNOWN.getCode())) {
                Date start = run.getStartTime();
                if (start == null && status.timestamp() != null && status.timestamp() > 0) {
                    start = new Date(status.timestamp());
                }
                int updated = runMapper.update(null, Wrappers.<Run>lambdaUpdate()
                        .eq(Run::getId, run.getId())
                        .eq(Run::getStatus, run.getStatus())
                        .set(Run::getStatus, RunStatusEnum.RUNNING.getCode())
                        .set(Run::getStartTime, start)
                        .set(Run::getUpdateTime, new Date()));
                if (updated > 0) {
                    log.info("run became running: runId={}, buildNo={}", run.getId(), status.buildNo());
                }
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
        if (terminal == RunStatusEnum.SUCCESS) {
            if (status.stages().isEmpty()) {
                markAllCiSucceeded(stages);
            } else {
                markPendingGateStages(stages);
            }
            markNotStartedSkipped(stages);
            runStageService.refreshRunStatus(run.getId());
        } else {
            markNonCiSkipped(stages);
            int updated = runMapper.update(null, Wrappers.<Run>lambdaUpdate()
                    .eq(Run::getId, run.getId())
                    .eq(Run::getStatus, run.getStatus())
                    .set(Run::getStatus, terminal.getCode())
                    .set(Run::getEndTime, endTime)
                    .set(Run::getUpdateTime, new Date()));
            if (updated == 0) {
                log.debug("run terminal status skipped, concurrent change: runId={}, expected={}",
                        run.getId(), run.getStatus());
            }
        }
        log.info("run status synced from Jenkins: runId={}, buildNo={}, status={}, result={}",
                run.getId(), status.buildNo(), terminal, result);
    }

    private void applyStageStatuses(List<RunStage> stages, List<JenkinsStageStatus> jenkinsStages) {
        List<RunStage> ciStages = stages.stream()
                .filter(stage -> stage.getType() != null && stage.getType() >= 1 && stage.getType() <= 4)
                .sorted(Comparator.comparingInt(stage -> stage.getSort() == null ? Integer.MAX_VALUE : stage.getSort()))
                .toList();
        List<JenkinsStageStatus> relevant = jenkinsStages.stream()
                .filter(stage -> StrUtil.isNotBlank(stage.name()))
                .filter(stage -> !stage.name().toLowerCase().contains("checkout"))
                .toList();
        Map<String, JenkinsStageStatus> byName = relevant.stream()
                .collect(Collectors.toMap(JenkinsStageStatus::name, stage -> stage, (left, right) -> right));
        for (int i = 0; i < ciStages.size(); i++) {
            RunStage stage = ciStages.get(i);
            if (StageStatusEnum.isTerminal(stage.getStatus())
                    || ObjUtil.equals(stage.getStatus(), StageStatusEnum.PENDING_GATE.getCode())
                    || ObjUtil.equals(stage.getStatus(), StageStatusEnum.BLOCKED.getCode())) {
                continue;
            }
            JenkinsStageStatus jenkinsStage = byName.get(stage.getName());
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
            Map<String, Object> detail = jenkinsDetail(jenkinsStage);
            switch (stageStatus) {
                case RUNNING -> runStageService.onStageRunning(stage.getRunId(), stage.getId(), new Date());
                case SUCCESS -> {
                    runStageService.onStageSucceeded(stage.getRunId(), stage.getId(), new Date(), detail);
                    syncTaskStatus(stage, StageStatusEnum.SUCCESS, detail);
                }
                case FAILURE, TIMEOUT, CANCELLED -> {
                    runStageService.onStageFailed(stage.getRunId(), stage.getId(), stageStatus, new Date(), detail);
                    syncTaskStatus(stage, stageStatus, detail);
                }
                case SKIPPED -> {
                    runStageService.transitionStage(stage.getId(), StageStatusEnum.SKIPPED, new Date());
                    syncTaskStatus(stage, StageStatusEnum.SKIPPED, detail);
                }
                default -> {
                }
            }
        }
    }

    private void syncTaskStatus(RunStage stage, StageStatusEnum stageStatus, Map<String, Object> detail) {
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
        StageStatusEnum taskTerminal = StageStatusEnum.of(taskStatus);
        for (RunStageTask task : taskMapper.selectByStage(stage.getId())) {
            if (!ObjUtil.equals(task.getStatus(), taskStatus)) {
                task.setStatus(taskStatus);
                task.setResult(StageResultUtil.task(taskTerminal, task, new Date(), detail));
                taskMapper.updateById(task);
            }
        }
    }

    /** Jenkins 阶段快照中的可用细节，随执行结果一起落 JSON */
    private Map<String, Object> jenkinsDetail(JenkinsStageStatus stage) {
        Map<String, Object> detail = new java.util.LinkedHashMap<>();
        detail.put("jenkinsStage", stage.name());
        detail.put("jenkinsStatus", stage.status());
        if (stage.startTimeMillis() != null) {
            detail.put("jenkinsStartTimeMillis", stage.startTimeMillis());
        }
        if (stage.durationMillis() != null) {
            detail.put("jenkinsDurationMillis", stage.durationMillis());
        }
        return detail;
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

    private void markAllCiSucceeded(List<RunStage> stages) {
        for (RunStage stage : stages) {
            if (stage.getType() == null || stage.getType() < 1 || stage.getType() > 4) {
                continue;
            }
            if (StageStatusEnum.isTerminal(stage.getStatus())) {
                continue;
            }
            runStageService.onStageSucceeded(stage.getRunId(), stage.getId(), new Date());
            syncTaskStatus(stage, StageStatusEnum.SUCCESS, null);
        }
    }

    /**
     * wfapi 可用但构建整体成功时，确保尚未上报终态的 CI 门禁阶段进入待门禁。
     */
    private void markPendingGateStages(List<RunStage> stages) {
        for (RunStage stage : stages) {
            if (stage.getType() == null || stage.getType() < 1 || stage.getType() > 4) {
                continue;
            }
            if (ObjUtil.equals(stage.getStatus(), StageStatusEnum.RUNNING.getCode())
                    || ObjUtil.equals(stage.getStatus(), StageStatusEnum.NOT_STARTED.getCode())) {
                runStageService.onStageSucceeded(stage.getRunId(), stage.getId(), new Date());
            }
        }
    }

    private void markNotStartedSkipped(List<RunStage> stages) {
        for (RunStage stage : stages) {
            // CI 阶段已由 markAllCiSucceeded / markPendingGateStages 处理，这里只跳过后置部署/发布阶段
            if (stage.getType() != null && (stage.getType() < 1 || stage.getType() > CI_STAGE_TYPE_MAX)
                    && ObjUtil.equals(stage.getStatus(), StageStatusEnum.NOT_STARTED.getCode())) {
                runStageService.transitionStage(stage.getId(), StageStatusEnum.SKIPPED, new Date());
                syncTaskStatus(stage, StageStatusEnum.SKIPPED, null);
            }
        }
    }

    private void markNonCiSkipped(List<RunStage> stages) {
        for (RunStage stage : stages) {
            if (stage.getType() != null && (stage.getType() < 1 || stage.getType() > 4)
                    && ObjUtil.equals(stage.getStatus(), StageStatusEnum.NOT_STARTED.getCode())) {
                runStageService.transitionStage(stage.getId(), StageStatusEnum.SKIPPED, new Date());
                syncTaskStatus(stage, StageStatusEnum.SKIPPED, null);
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
