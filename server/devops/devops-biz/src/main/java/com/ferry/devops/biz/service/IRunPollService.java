package com.ferry.devops.biz.service;

/**
 * 运行状态轮询：从 Jenkins 拉取构建与阶段状态，更新运行记录。
 */
public interface IRunPollService {

    /**
     * 轮询活动运行（排队中/运行中）并同步 Jenkins 状态。
     *
     * <p>兜底对账通道，调度频率 60 秒级；主通道为脚本内 webhook 回调，
     * 轮询补正丢失的回调与整条构建的最终收敛。
     */
    void pollActiveRuns();

    /**
     * 轮询结果待确认的运行并同步 Jenkins 状态。
     *
     * <p>低频降档，调度频率 300 秒级；触发请求结果不确定时兜底收敛，
     * 状态推进依赖构建号/队列号解析，无高频变化需求。
     */
    void pollUnknownRuns();
}
