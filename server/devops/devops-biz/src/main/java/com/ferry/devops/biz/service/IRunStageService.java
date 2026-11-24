package com.ferry.devops.biz.service;

import java.util.Date;
import java.util.Map;

import com.ferry.devops.api.enums.StageStatusEnum;

/**
 * 运行/阶段状态服务：统一阶段迁移与运行状态收敛。
 */
public interface IRunStageService {

    /** Jenkins 阶段开始执行 */
    void onStageRunning(Long runId, Long stageId, Date time);

    /** Jenkins 阶段执行成功；单测/静态扫描阶段按门禁配置进入待门禁 */
    default void onStageSucceeded(Long runId, Long stageId, Date time) {
        onStageSucceeded(runId, stageId, time, null);
    }

    /** Jenkins 阶段执行成功，附带 Jenkins 结果细节写入 result */
    void onStageSucceeded(Long runId, Long stageId, Date time, Map<String, Object> detail);

    /** Jenkins 阶段失败/超时/取消 */
    default void onStageFailed(Long runId, Long stageId, StageStatusEnum terminal, Date time) {
        onStageFailed(runId, stageId, terminal, time, null);
    }

    /** Jenkins 阶段失败/超时/取消，附带 Jenkins 结果细节写入 result */
    void onStageFailed(Long runId, Long stageId, StageStatusEnum terminal, Date time, Map<String, Object> detail);

    /** 按状态机迁移阶段状态（供门禁/豁免使用） */
    void transitionStage(Long stageId, StageStatusEnum target, Date time);

    /** 由阶段状态和门禁结果重新收敛运行状态 */
    void refreshRunStatus(Long runId);

    /** 门禁结果变化后重算阶段与运行 */
    void refreshAfterGateChange(Long runId, Long runStageId);

    /** 最近一条干预记录是否为人工暂停 */
    boolean isManualPaused(Long runId);

    /** 是否允许人工继续（无门禁阻塞、无阻塞/待门禁阶段、最近操作为暂停） */
    boolean canResumeManually(Long runId);
}
