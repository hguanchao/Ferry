package com.ferry.devops.biz.integration.jenkins;

/**
 * Jenkins 阶段下发结果。
 *
 * @param jobName Job 名，直接取应用编码
 * @param queueId Jenkins 队列项 id，未取得时为 null
 * @param buildNo Jenkins 构建号，队列尚未分配执行器时为 null
 */
public record JenkinsDispatchResult(String jobName, Long queueId, Long buildNo) {
}
