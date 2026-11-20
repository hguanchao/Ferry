package com.ferry.devops.biz.integration.jenkins;

/**
 * Jenkins 客户端：同步 Pipeline Job 配置、触发构建、查询构建状态、中止构建。
 *
 * <p>职责分离：Job 配置的创建/更新只发生在保存流水线后的同步编排（{@link #syncJob}），
 * 触发构建要求 Job 已同步，不再在触发路径自动建 Job。Job 名为
 * {@code <应用编码>-pipeline-<流水线id>}，由 {@link JenkinsJobNames#pipelineJobName} 拼接校验。
 * baseUrl 配置为空时降级为仅记录日志（模拟）。
 */
public interface JenkinsClient {

    /**
     * 创建或更新 Pipeline Job 配置。
     *
     * <p>更新前读取现有 config.xml 验证类型与归属（{@link JenkinsJobXml#MANAGED_MARKER_PREFIX}），
     * 仅修改 Ferry 托管的脚本、Ferry 参数与禁用位，保留其余配置；同名非 Ferry 管理 Job 拒绝覆盖。
     *
     * @param jobName  完整 Job 名（{@link JenkinsJobNames#pipelineJobName} 生成）
     * @param appCode  应用编码（写入 description 便于辨认）
     * @param script   完整声明式脚本（与预览同源）
     * @param disabled 是否禁用（待配置流水线为 true）
     * @return 下发结果（Job 名；同步不涉及队列与构建号）
     */
    JenkinsDispatchResult syncJob(String jobName, String appCode, String script, boolean disabled);

    /**
     * 触发一次完整 CI 构建（要求 Job 已同步，不存在时报明确错误，不再自动建 Job）。
     *
     * @param runId    运行id
     * @param snapshot 版本快照 JSON
     * @param jobName  完整 Job 名
     * @param source   代码源（应用编码、仓库地址、分支、提交/引用），作为构建参数下发
     * @return 下发结果（Job 名、队列项、构建号）
     */
    JenkinsDispatchResult triggerBuild(Long runId, String snapshot, String jobName, BuildSource source);

    /**
     * 通过队列项解析构建号。
     *
     * @param jobName Job 名
     * @param queueId Jenkins 队列项 id
     * @return 构建号；队列尚未分配执行器时返回 null
     */
    Long resolveQueueBuild(String jobName, Long queueId);

    /**
     * 查询构建及阶段状态。
     *
     * @param jobName Job 名
     * @param buildNo 构建号
     * @return 构建状态快照
     */
    JenkinsBuildStatus buildStatus(String jobName, Long buildNo);

    /**
     * 中止构建。
     *
     * @param jobName Job 名
     * @param buildNo 构建号；未分配时不做任何事
     */
    void abortBuild(String jobName, Long buildNo);
}
