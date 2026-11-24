package com.ferry.devops.biz.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.dto.query.RunStageQuery;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunOperation;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.entity.RunStageTask;
import com.ferry.devops.api.enums.DevopsStateMachine;
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.api.vo.RunStageTaskVO;
import com.ferry.devops.api.vo.RunStageVO;
import com.ferry.devops.biz.config.DevopsProperties;
import com.ferry.devops.biz.mapper.RunStageGateMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunOperationMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.mapper.RunStageTaskMapper;
import com.ferry.devops.biz.service.IRunStageService;
import com.ferry.devops.biz.service.IRunStageTaskService;
import com.ferry.devops.biz.utils.StageResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 运行/阶段状态服务实现。
 *
 * <p>阶段状态是业务事实，运行状态由阶段和门禁结果收敛；单测/静态扫描阶段执行成功后
 * 先进入“待门禁”，门禁裁决通过才转为成功、不通过则阻塞。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunStageServiceImpl extends ServiceImpl<RunStageMapper, RunStage> implements IRunStageService {

    private static final int CI_STAGE_TYPE_MAX = 4;
    private static final int OPERATION_PAUSE = 1;

    private final RunMapper runMapper;
    private final RunStageMapper stageMapper;
    private final RunStageTaskMapper taskMapper;
    private final RunOperationMapper operationMapper;
    private final RunStageGateMapper gateResultMapper;
    private final DevopsProperties properties;
    private final IRunStageTaskService runStageTaskService;

    @Override
    public List<RunStageVO> list(RunStageQuery body) {
        Long runId = body.getRunId();
        List<RunStage> runStages = list(Wrappers.<RunStage>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(runId), RunStage::getRunId, runId));
        if (CollUtil.isEmpty(runStages)) {
            return Collections.emptyList();
        }
        Set<Long> runStageIds = runStages.stream().map(RunStage::getId).collect(Collectors.toSet());
        List<RunStageTask> runStageTasks = runStageTaskService.list(Wrappers.<RunStageTask>lambdaQuery()
                .in(RunStageTask::getRunStageId, runStageIds));
        Map<Long, List<RunStageTaskVO>> runStageTaskVOMap = runStageTasks.stream().collect(Collectors.groupingBy(
                RunStageTask::getRunStageId,
                Collectors.mapping(task -> BeanUtil.copy(task, RunStageTaskVO.class), Collectors.toList())
        ));
        List<RunStageVO> runStageVOList = BeanUtil.copy(runStages, RunStageVO.class);
        runStageVOList.forEach(vo -> vo.setTasks(runStageTaskVOMap.get(vo.getId())));
        return runStageVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onStageRunning(Long runId, Long stageId, Date time) {
        RunStage stage = requireStage(runId, stageId);
        Integer current = stage.getStatus();
        if (StageStatusEnum.isTerminal(current)
                || ObjUtil.equals(current, StageStatusEnum.PENDING_GATE.getCode())
                || ObjUtil.equals(current, StageStatusEnum.BLOCKED.getCode())) {
            return;
        }
        if (!ObjUtil.equals(current, StageStatusEnum.RUNNING.getCode())) {
            DevopsStateMachine.checkStage(current, StageStatusEnum.RUNNING.getCode());
            stage.setStatus(StageStatusEnum.RUNNING.getCode());
        }
        if (stage.getStartTime() == null) {
            stage.setStartTime(time);
        }
        stageMapper.updateById(stage);
        refreshRunStatus(runId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onStageSucceeded(Long runId, Long stageId, Date time, Map<String, Object> detail) {
        RunStage stage = requireStage(runId, stageId);
        Integer current = stage.getStatus();
        if (StageStatusEnum.isTerminal(current)
                || ObjUtil.equals(current, StageStatusEnum.PENDING_GATE.getCode())
                || ObjUtil.equals(current, StageStatusEnum.BLOCKED.getCode())) {
            return;
        }
        boolean gateStage = isGateStage(stage.getType()) && gateEnabled();
        StageStatusEnum target = gateStage ? StageStatusEnum.PENDING_GATE : StageStatusEnum.SUCCESS;
        DevopsStateMachine.checkStage(current, target.getCode());
        stage.setStatus(target.getCode());
        applyStageTimes(stage, time, detail);
        stage.setResult(StageResultUtil.execution(stage.getResult(), target,
                stage.getStartTime(), stage.getEndTime(), detail));
        stageMapper.updateById(stage);
        refreshRunStatus(runId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onStageFailed(Long runId, Long stageId, StageStatusEnum terminal, Date time,
                              Map<String, Object> detail) {
        if (terminal == null || !StageStatusEnum.isTerminal(terminal.getCode())) {
            return;
        }
        RunStage stage = requireStage(runId, stageId);
        Integer current = stage.getStatus();
        if (StageStatusEnum.isTerminal(current)) {
            return;
        }
        DevopsStateMachine.checkStage(current, terminal.getCode());
        stage.setStatus(terminal.getCode());
        applyStageTimes(stage, time, detail);
        stage.setResult(StageResultUtil.execution(stage.getResult(), terminal,
                stage.getStartTime(), stage.getEndTime(), detail));
        stageMapper.updateById(stage);
        refreshRunStatus(runId);
    }

    /**
     * 写入阶段起止时间：优先用 Jenkins 上报的真实时间戳（jenkinsStartTimeMillis +
     * jenkinsDurationMillis），避免轮询间隔造成的"构建60s、其余0s"失真；Jenkins 未提供时回退轮询时间。
     */
    private void applyStageTimes(RunStage stage, Date pollTime, Map<String, Object> detail) {
        Long startMs = detail == null ? null : asLong(detail.get("jenkinsStartTimeMillis"));
        Long durationMs = detail == null ? null : asLong(detail.get("jenkinsDurationMillis"));
        if (startMs != null) {
            stage.setStartTime(new Date(startMs));
            if (durationMs != null) {
                stage.setEndTime(new Date(startMs + durationMs));
            } else {
                stage.setEndTime(pollTime);
            }
            return;
        }
        if (stage.getStartTime() == null) {
            stage.setStartTime(pollTime);
        }
        stage.setEndTime(pollTime);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transitionStage(Long stageId, StageStatusEnum target, Date time) {
        RunStage stage = stageMapper.selectById(stageId);
        if (stage == null || target == null) {
            return;
        }
        Integer current = stage.getStatus();
        if (ObjUtil.equals(current, target.getCode()) || StageStatusEnum.isTerminal(current)) {
            return;
        }
        DevopsStateMachine.checkStage(current, target.getCode());
        stage.setStatus(target.getCode());
        if (target == StageStatusEnum.RUNNING) {
            if (stage.getStartTime() == null) {
                stage.setStartTime(time);
            }
        } else if (target != StageStatusEnum.NOT_STARTED) {
            if (stage.getStartTime() == null) {
                stage.setStartTime(time);
            }
            if (stage.getEndTime() == null) {
                stage.setEndTime(time);
            }
        }
        if (target != StageStatusEnum.RUNNING && target != StageStatusEnum.NOT_STARTED) {
            stage.setResult(StageResultUtil.execution(stage.getResult(), target,
                    stage.getStartTime(), stage.getEndTime(), null));
        }
        stageMapper.updateById(stage);
        refreshRunStatus(stage.getRunId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshRunStatus(Long runId) {
        Run run = runMapper.selectById(runId);
        if (run == null || isTerminalRun(run.getStatus())) {
            return;
        }
        List<RunStage> stages = stageMapper.selectByRun(runId);
        Integer current = run.getStatus();
        Integer target = resolveRunStatus(runId, run, stages);
        if (ObjUtil.equals(current, target)) {
            return;
        }
        DevopsStateMachine.checkRun(current, target);
        Date now = new Date();
        if (ObjUtil.equals(target, RunStatusEnum.SUCCESS.getCode())) {
            skipNonCiNotStarted(stages, now);
        }
        Date start = run.getStartTime() == null ? minStart(stages) : run.getStartTime();
        Date end = isTerminalRun(target) ? maxEnd(stages) : null;
        int updated = runMapper.update(null, Wrappers.<Run>lambdaUpdate()
                .eq(Run::getId, runId)
                .eq(Run::getStatus, current)
                .set(Run::getStatus, target)
                .set(Run::getStartTime, start)
                .set(Run::getEndTime, end)
                .set(Run::getUpdateTime, new Date()));
        if (updated == 0) {
            log.debug("run status concurrent changed, skip: runId={}, expected={}, target={}",
                    runId, current, target);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshAfterGateChange(Long runId, Long runStageId) {
        RunStage stage = stageMapper.selectById(runStageId);
        if (stage != null
                && isGateStage(stage.getType())
                && ObjUtil.equals(stage.getStatus(), StageStatusEnum.BLOCKED.getCode())
                && gateResultMapper.countBlockingByStage(runStageId) == 0) {
            transitionStage(runStageId, StageStatusEnum.SUCCESS, new Date());
        }
        refreshRunStatus(runId);
    }

    @Override
    public boolean isManualPaused(Long runId) {
        RunOperation latest = operationMapper.selectLatestByRun(runId);
        return latest != null && ObjUtil.equals(latest.getType(), OPERATION_PAUSE);
    }

    @Override
    public boolean canResumeManually(Long runId) {
        Run run = runMapper.selectById(runId);
        if (run == null || !ObjUtil.equals(run.getStatus(), RunStatusEnum.BLOCKED.getCode())) {
            return false;
        }
        if (gateResultMapper.countBlocking(runId) > 0) {
            return false;
        }
        boolean stageBlocked = stageMapper.selectByRun(runId).stream()
                .anyMatch(stage -> ObjUtil.equals(stage.getStatus(), StageStatusEnum.BLOCKED.getCode())
                        || ObjUtil.equals(stage.getStatus(), StageStatusEnum.PENDING_GATE.getCode()));
        return !stageBlocked && isManualPaused(runId);
    }

    private Integer resolveRunStatus(Long runId, Run run, List<RunStage> stages) {
        if (stages.stream().anyMatch(stage -> ObjUtil.equals(stage.getStatus(), StageStatusEnum.FAILURE.getCode()))) {
            return RunStatusEnum.FAILURE.getCode();
        }
        if (stages.stream().anyMatch(stage -> ObjUtil.equals(stage.getStatus(), StageStatusEnum.TIMEOUT.getCode()))) {
            return RunStatusEnum.TIMEOUT.getCode();
        }
        if (stages.stream().anyMatch(stage -> ObjUtil.equals(stage.getStatus(), StageStatusEnum.CANCELLED.getCode()))) {
            return RunStatusEnum.CANCELLED.getCode();
        }
        boolean gateBlocked = gateResultMapper.countBlocking(runId) > 0;
        boolean stageBlocked = stages.stream()
                .anyMatch(stage -> ObjUtil.equals(stage.getStatus(), StageStatusEnum.BLOCKED.getCode()));
        if (gateBlocked || stageBlocked || isManualPaused(runId)) {
            return RunStatusEnum.BLOCKED.getCode();
        }
        boolean active = stages.stream().anyMatch(stage ->
                ObjUtil.equals(stage.getStatus(), StageStatusEnum.PENDING_GATE.getCode())
                        || ObjUtil.equals(stage.getStatus(), StageStatusEnum.RUNNING.getCode()));
        if (active) {
            return RunStatusEnum.RUNNING.getCode();
        }
        List<RunStage> ciStages = stages.stream()
                .filter(stage -> stage.getType() != null && stage.getType() >= 1 && stage.getType() <= CI_STAGE_TYPE_MAX)
                .toList();
        boolean allCiDone = !ciStages.isEmpty() && ciStages.stream().allMatch(stage ->
                ObjUtil.equals(stage.getStatus(), StageStatusEnum.SUCCESS.getCode())
                        || ObjUtil.equals(stage.getStatus(), StageStatusEnum.SKIPPED.getCode())
                        || ObjUtil.equals(stage.getStatus(), StageStatusEnum.REUSED.getCode()));
        if (allCiDone) {
            return RunStatusEnum.SUCCESS.getCode();
        }
        return run.getStatus();
    }

    private RunStage requireStage(Long runId, Long stageId) {
        RunStage stage = stageMapper.selectById(stageId);
        if (stage == null || !ObjUtil.equals(stage.getRunId(), runId)) {
            throw new com.ferry.common.core.exception.ServiceException("运行阶段不存在");
        }
        return stage;
    }

    private boolean gateEnabled() {
        return properties.getGate() != null && properties.getGate().isEnabled();
    }

    private boolean isGateStage(Integer stageType) {
        return ObjUtil.equals(stageType, 2) || ObjUtil.equals(stageType, 3);
    }

    private boolean isTerminalRun(Integer status) {
        return ObjUtil.equals(status, RunStatusEnum.SUCCESS.getCode())
                || ObjUtil.equals(status, RunStatusEnum.FAILURE.getCode())
                || ObjUtil.equals(status, RunStatusEnum.TIMEOUT.getCode())
                || ObjUtil.equals(status, RunStatusEnum.CANCELLED.getCode());
    }

    private void skipNonCiNotStarted(List<RunStage> stages, Date now) {
        for (RunStage stage : stages) {
            if (stage.getType() != null && stage.getType() > CI_STAGE_TYPE_MAX
                    && ObjUtil.equals(stage.getStatus(), StageStatusEnum.NOT_STARTED.getCode())) {
                stage.setStatus(StageStatusEnum.SKIPPED.getCode());
                stage.setEndTime(now);
                stage.setResult(StageResultUtil.execution(stage.getResult(), StageStatusEnum.SKIPPED,
                        stage.getStartTime(), now, null));
                stageMapper.updateById(stage);
                for (RunStageTask task : taskMapper.selectByStage(stage.getId())) {
                    if (!ObjUtil.equals(task.getStatus(), StageStatusEnum.SKIPPED.getCode())) {
                        task.setStatus(StageStatusEnum.SKIPPED.getCode());
                        task.setResult(StageResultUtil.task(StageStatusEnum.SKIPPED, task, now, null));
                        taskMapper.updateById(task);
                    }
                }
            }
        }
    }

    private Date minStart(List<RunStage> stages) {
        return stages.stream()
                .map(RunStage::getStartTime)
                .filter(java.util.Objects::nonNull)
                .min(Date::compareTo)
                .orElse(null);
    }

    private Date maxEnd(List<RunStage> stages) {
        return stages.stream()
                .map(RunStage::getEndTime)
                .filter(java.util.Objects::nonNull)
                .max(Date::compareTo)
                .orElse(new Date());
    }
}
