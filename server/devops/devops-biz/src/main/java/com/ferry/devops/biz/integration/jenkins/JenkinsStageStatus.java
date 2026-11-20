package com.ferry.devops.biz.integration.jenkins;

/**
 * Jenkins 单个阶段的状态快照。
 *
 * @param name            Jenkins 阶段名
 * @param status          SUCCESS/FAILED/IN_PROGRESS/ABORTED/NOT_EXECUTED/UNSTABLE 等
 * @param startTimeMillis 开始时间毫秒
 * @param durationMillis  耗时毫秒
 */
public record JenkinsStageStatus(String name, String status, Long startTimeMillis, Long durationMillis) {
}
