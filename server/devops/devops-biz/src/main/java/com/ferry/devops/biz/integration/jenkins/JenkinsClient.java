package com.ferry.devops.biz.integration.jenkins;

/**
 * Jenkins 客户端：触发整条 CI 流水线、查询构建状态、中止构建。
 *
 * <p>每个应用对应一个 Job，Job 名直接使用应用编码；一次运行只触发一次构建，
 * 由 Jenkinsfile 内部顺序执行多个 CI 阶段。Ferry 通过定时轮询构建/阶段状态推进运行，
 * 不依赖 Jenkins 主动回调。baseUrl 配置为空时降级为仅记录日志。
 */
public interface JenkinsClient {

    /**
     * 触发一次完整 CI 构建
     *
     * @param runId     运行id
     * @param snapshot  版本快照 JSON
     * @param source    代码源（应用编码、仓库地址、分支、提交/引用）
     * @param stageSpec CI 阶段与任务脚本 JSON，由 Jenkinsfile 动态生成 Stage
     * @return 下发结果（Job 名、队列项、构建号）
     */
    JenkinsDispatchResult triggerBuild(Long runId, String snapshot, BuildSource source, String stageSpec);

    /**
     * 通过队列项解析构建号。
     *
     * @param appCode 应用编码（即 Job 名）
     * @param queueId Jenkins 队列项 id
     * @return 构建号；队列尚未分配执行器时返回 null
     */
    Long resolveQueueBuild(String appCode, Long queueId);

    /**
     * 查询构建及阶段状态。
     *
     * @param appCode 应用编码（即 Job 名）
     * @param buildNo 构建号
     * @return 构建状态快照
     */
    JenkinsBuildStatus buildStatus(String appCode, Long buildNo);

    /**
     * 中止构建
     *
     * @param runId   运行id
     * @param buildNo 构建号
     */
    void abortBuild(Long runId, Long buildNo);
}
