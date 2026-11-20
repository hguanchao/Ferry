package com.ferry.devops.biz.integration.repo;

/**
 * 代码平台返回的提交摘要。
 *
 * @param sha         完整提交 SHA
 * @param shortSha    短 SHA；平台未返回时由完整 SHA 截取
 * @param message     提交说明（仅首行）
 * @param authorName  提交人
 * @param committedAt 提交时间（ISO 8601 字符串，展示格式化由前端负责）
 */
public record RepoCommit(String sha, String shortSha, String message, String authorName, String committedAt) {
}
