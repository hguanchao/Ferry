package com.ferry.devops.biz.integration.jenkins;

/**
 * Jenkins 阶段下发使用的触发参数。
 *
 * <p>应用编码同时作为 Jenkins Job 名，仓库地址随构建参数下发；
 * 仓库令牌不进入 Jenkins 参数，私有仓库由 Jenkins 侧凭据完成签出。
 * 运行状态与门禁结果统一由 Ferry 轮询/归档拉取，脚本不注入任何回调逻辑。
 *
 * @param appCode  应用编码
 * @param repoUrl  仓库 Git 地址
 * @param branch   分支
 * @param commitId 提交或引用值
 * @param refType  引用类型 1 Commit 2 Tag/Ref
 * @param refValue 原始引用值
 */
public record BuildSource(String appCode, String repoUrl, String branch, String commitId,
                          Integer refType, String refValue) {
}
