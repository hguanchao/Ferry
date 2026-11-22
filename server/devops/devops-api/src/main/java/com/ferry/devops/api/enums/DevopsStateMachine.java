package com.ferry.devops.api.enums;

import com.ferry.common.core.exception.ServiceException;

import java.util.Map;

/**
 * 交付域状态机
 *
 * <p>集中声明各状态机允许的前驱状态，由 Service 单点校验，避免流转规则散落各处。
 */
public final class DevopsStateMachine {

    /**
     * 运行状态允许的前驱：key 为目标状态，value 为允许迁移到此状态的前驱集合
     */
    private static final Map<Integer, int[]> RUN_TRANSITIONS = Map.of(
            RunStatusEnum.RUNNING.getCode(), new int[]{
                    RunStatusEnum.QUEUED.getCode(), RunStatusEnum.BLOCKED.getCode(), RunStatusEnum.UNKNOWN.getCode()},
            RunStatusEnum.BLOCKED.getCode(), new int[]{
                    RunStatusEnum.QUEUED.getCode(), RunStatusEnum.RUNNING.getCode(), RunStatusEnum.UNKNOWN.getCode()},
            RunStatusEnum.SUCCESS.getCode(), new int[]{
                    RunStatusEnum.QUEUED.getCode(), RunStatusEnum.RUNNING.getCode(),
                    RunStatusEnum.BLOCKED.getCode(), RunStatusEnum.UNKNOWN.getCode()},
            RunStatusEnum.FAILURE.getCode(), new int[]{
                    RunStatusEnum.QUEUED.getCode(), RunStatusEnum.RUNNING.getCode(),
                    RunStatusEnum.BLOCKED.getCode(), RunStatusEnum.UNKNOWN.getCode()},
            RunStatusEnum.TIMEOUT.getCode(), new int[]{
                    RunStatusEnum.RUNNING.getCode(), RunStatusEnum.BLOCKED.getCode(), RunStatusEnum.UNKNOWN.getCode()},
            RunStatusEnum.CANCELLED.getCode(), new int[]{
                    RunStatusEnum.QUEUED.getCode(), RunStatusEnum.RUNNING.getCode(),
                    RunStatusEnum.BLOCKED.getCode(), RunStatusEnum.UNKNOWN.getCode()},
            RunStatusEnum.UNKNOWN.getCode(), new int[]{RunStatusEnum.QUEUED.getCode(), RunStatusEnum.RUNNING.getCode()}
    );

    /**
     * 阶段状态允许的前驱：key 为目标状态，value 为允许迁移到此状态的前驱集合
     */
    private static final Map<Integer, int[]> STAGE_TRANSITIONS = Map.of(
            StageStatusEnum.RUNNING.getCode(), new int[]{StageStatusEnum.NOT_STARTED.getCode()},
            StageStatusEnum.PENDING_GATE.getCode(), new int[]{
                    StageStatusEnum.NOT_STARTED.getCode(), StageStatusEnum.RUNNING.getCode()},
            StageStatusEnum.SUCCESS.getCode(), new int[]{
                    StageStatusEnum.NOT_STARTED.getCode(), StageStatusEnum.RUNNING.getCode(),
                    StageStatusEnum.PENDING_GATE.getCode(), StageStatusEnum.BLOCKED.getCode(),
                    StageStatusEnum.REUSED.getCode()},
            StageStatusEnum.FAILURE.getCode(), new int[]{
                    StageStatusEnum.NOT_STARTED.getCode(), StageStatusEnum.RUNNING.getCode(),
                    StageStatusEnum.PENDING_GATE.getCode(), StageStatusEnum.BLOCKED.getCode()},
            StageStatusEnum.BLOCKED.getCode(), new int[]{
                    StageStatusEnum.RUNNING.getCode(), StageStatusEnum.PENDING_GATE.getCode()},
            StageStatusEnum.SKIPPED.getCode(), new int[]{
                    StageStatusEnum.NOT_STARTED.getCode(), StageStatusEnum.RUNNING.getCode()},
            StageStatusEnum.TIMEOUT.getCode(), new int[]{
                    StageStatusEnum.NOT_STARTED.getCode(), StageStatusEnum.RUNNING.getCode(),
                    StageStatusEnum.PENDING_GATE.getCode(), StageStatusEnum.BLOCKED.getCode()},
            StageStatusEnum.REUSED.getCode(), new int[]{StageStatusEnum.NOT_STARTED.getCode()},
            StageStatusEnum.CANCELLED.getCode(), new int[]{
                    StageStatusEnum.NOT_STARTED.getCode(), StageStatusEnum.RUNNING.getCode(),
                    StageStatusEnum.PENDING_GATE.getCode(), StageStatusEnum.BLOCKED.getCode()}
    );

    private DevopsStateMachine() {
    }

    /**
     * 校验运行状态流转是否合法
     *
     * @param from 当前状态
     * @param to   目标状态
     */
    public static void checkRun(Integer from, Integer to) {
        check(RUN_TRANSITIONS, from, to);
    }

    /**
     * 校验阶段状态流转是否合法
     *
     * @param from 当前状态
     * @param to   目标状态
     */
    public static void checkStage(Integer from, Integer to) {
        if (from != null && from.equals(to)) {
            return;
        }
        check(STAGE_TRANSITIONS, from, to);
    }

    private static void check(Map<Integer, int[]> transitions, Integer from, Integer to) {
        int[] allowed = transitions.get(to);
        if (allowed == null) {
            return;
        }
        for (int prev : allowed) {
            if (prev == (from == null ? -1 : from)) {
                return;
            }
        }
        throw new ServiceException("当前状态不允许该操作");
    }
}
