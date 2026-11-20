package com.ferry.devops.biz.integration.jenkins;

import java.util.List;

/**
 * Jenkins 构建状态快照。
 *
 * @param buildNo   Jenkins 构建号
 * @param building  是否仍在构建
 * @param result    构建结果，构建中为 null
 * @param timestamp 构建开始时间毫秒
 * @param duration  构建耗时毫秒
 * @param stages    阶段状态列表，wfapi 不可用时为空
 */
public record JenkinsBuildStatus(Long buildNo, boolean building, String result,
                                 Long timestamp, Long duration, List<JenkinsStageStatus> stages) {
}
