package com.ferry.devops.biz.integration.repo;

/**
 * 代码平台访问参数。
 *
 * @param repoType    代码平台 1 GitLab 2 GitHub 3 Gitee
 * @param scheme      HTTP 协议，固定 http/https
 * @param host        代码平台域名
 * @param port        HTTP 端口，-1 表示默认端口
 * @param projectPath 项目路径，GitLab 支持多级 group，GitHub/Gitee 为 owner/repo
 * @param token       访问令牌，公开仓库可为空
 */
public record RepoTarget(int repoType, String scheme, String host, int port, String projectPath, String token) {

    /**
     * 带端口的 host，用于拼 API 基址。
     *
     * @return host 或 host:port
     */
    public String authority() {
        return port > 0 ? host + ":" + port : host;
    }
}
