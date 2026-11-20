package com.ferry.devops.biz.service;

/**
 * 运行状态轮询：从 Jenkins 拉取构建与阶段状态，更新运行记录。
 */
public interface IRunPollService {

    /**
     * 轮询活动/结果待确认运行并同步 Jenkins 状态。
     */
    void pollActiveRuns();
}
