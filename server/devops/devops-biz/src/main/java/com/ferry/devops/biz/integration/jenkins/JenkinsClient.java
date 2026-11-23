package com.ferry.devops.biz.integration.jenkins;

import com.ferry.devops.api.model.JenkinsJob;

/**
 * Jenkins 客户端：同步 Pipeline Job 配置、触发构建、查询构建状态、中止构建、删除 Job。
 *
 * <p>职责分离：Job 配置的创建/更新只发生在保存流水线后的同步编排（{@link #syncJob}），
 * 触发构建要求 Job 已同步，不再在触发路径自动建 Job。Job 名为
 * {@code <应用编码>}，由 {@link JenkinsJobNames#appJobName} 拼接校验；应用创建时初始化空白
 * Job、删除应用时同步删除。baseUrl 配置为空时降级为仅记录日志（模拟）。
 */
public interface JenkinsClient {

    /**
     * 创建 Job（幂等）：先查 Job 是否存在，存在直接完成返回，不存在时以最小配置创建。
     *
     * <p>仅负责"让 Job 存在"，不更新既有 Job 的脚本与属性——脚本更新走 {@link #updateJob}。
     *
     * @param job Job 配置：名称为完整 Job 名（{@link JenkinsJobNames#appJobName} 生成），
     *            应用编码写入 description 便于辨认，脚本为完整声明式脚本（与预览同源），
     *            禁用位标识待配置流水线，仓库信息写入 GitHub 项目属性（地址为空则不生成该属性）
     */
    void createJob(JenkinsJob job);

    /**
     * 更新 Job 配置（要求 Job 已存在）。
     *
     * <p>更新前读取现有 config.xml 验证类型与归属（{@link JenkinsJobXml#MANAGED_MARKER_PREFIX}），
     * 仅修改 Ferry 托管的脚本、Ferry 参数、禁用位与 GitHub 项目属性，保留其余配置；
     * 同名非 Ferry 管理 Job 拒绝覆盖。
     *
     * @param job Job 配置：名称为完整 Job 名（{@link JenkinsJobNames#appJobName} 生成），
     *            应用编码写入 description 便于辨认，脚本为完整声明式脚本（与预览同源），
     *            禁用位标识停用状态，仓库信息写入 GitHub 项目属性（地址为空则不保留该属性）
     * @throws com.ferry.common.core.exception.ServiceException Job 不存在（未创建/未同步）时
     */
    void updateJob(JenkinsJob job);

    /**
     * 删除 Job（应用删除时清理裸 Job）。
     *
     * @param jobName 完整 Job 名
     * @throws com.ferry.common.core.exception.ServiceException Job 存在但删除被拒绝时
     */
    void deleteJob(String jobName);

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
     * 读取 Jenkins 构建归档产物。
     *
     * @param jobName      Job 名
     * @param buildNo      构建号
     * @param artifactPath 归档相对路径，如 reports/ferry-gate-summary.json
     * @return 文件内容；404 返回 empty，其他失败抛出异常
     */
    java.util.Optional<String> fetchArtifact(String jobName, Long buildNo, String artifactPath);

    /**
     * 中止构建。
     *
     * @param jobName Job 名
     * @param buildNo 构建号；未分配时不做任何事
     */
    void abortBuild(String jobName, Long buildNo);
}
