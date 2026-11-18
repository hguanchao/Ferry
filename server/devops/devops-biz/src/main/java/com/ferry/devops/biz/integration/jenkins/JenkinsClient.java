package com.ferry.devops.biz.integration.jenkins;

/**
 * Jenkins 客户端：下发 CI 阶段 Job、中止构建。
 *
 * <p>baseUrl 配置为空时降级为仅记录日志（本地/无 Jenkins 环境仍可编译运行）；
 * 配置后走真实 HTTP，用 user:token 做 Basic 认证，失败收敛为 {@link com.ferry.common.core.exception.ServiceException}，
 * 由事务提交后的调用方决定重试/告警，不回滚运行记录。
 */
public interface JenkinsClient {

    /**
     * 下发 CI 阶段 Job
     *
     * @param runId    运行id
     * @param stageId  阶段id
     * @param jobId    Job 标识
     * @param snapshot 版本快照 JSON
     */
    void dispatchStage(Long runId, Long stageId, String jobId, String snapshot);

    /**
     * 中止构建
     *
     * @param runId   运行id
     * @param buildNo 构建号
     */
    void abortBuild(Long runId, Long buildNo);
}
