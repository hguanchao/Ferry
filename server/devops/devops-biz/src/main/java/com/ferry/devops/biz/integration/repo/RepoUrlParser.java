package com.ferry.devops.biz.integration.repo;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.devops.api.entity.App;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 从应用登记的仓库地址解析代码平台访问参数。
 *
 * <p>支持 HTTP(S)、git@host:path 与 ssh:// 三种写法；SSH 地址按 HTTPS 访问，
 * 端口不沿用 SSH 端口。只允许 http/https 协议，避免把内部服务地址当仓库访问。
 */
public final class RepoUrlParser {

    private static final int REPO_TYPE_GITLAB = 1;
    private static final int REPO_TYPE_GITHUB = 2;
    private static final int REPO_TYPE_GITEE = 3;
    private static final String GIT_SUFFIX = ".git";

    private RepoUrlParser() {
    }

    /**
     * 解析应用登记的仓库地址。
     *
     * @param app 应用档案
     * @return 代码平台访问参数
     * @throws ServiceException 应用不存在、未登记仓库地址或地址格式不合法
     */
    public static RepoTarget parse(App app) {
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        int repoType = app.getRepoType() == null ? 0 : app.getRepoType();
        if (repoType < REPO_TYPE_GITLAB || repoType > REPO_TYPE_GITEE) {
            throw new ServiceException("应用未登记有效的代码平台");
        }
        String raw = app.getRepoUrl() == null ? "" : app.getRepoUrl().trim();
        if (raw.isEmpty()) {
            throw new ServiceException("应用未登记仓库地址");
        }
        RepoUrl repoUrl = normalize(raw);
        if (repoUrl.host() == null || repoUrl.host().isBlank()) {
            throw new ServiceException("仓库地址格式不正确，缺少域名");
        }
        String projectPath = resolveProjectPath(repoType, repoUrl.path());
        return new RepoTarget(repoType, repoUrl.scheme(), repoUrl.host(), repoUrl.port(),
                projectPath, app.getRepoToken());
    }

    private static RepoUrl normalize(String raw) {
        // git@host:group/repo.git：SSH 简写没有协议头，按 HTTPS 访问
        if (raw.startsWith("git@")) {
            int colon = raw.indexOf(':');
            if (colon <= 4) {
                throw new ServiceException("仓库地址格式不正确");
            }
            return new RepoUrl("https", raw.substring(4, colon), -1, raw.substring(colon + 1));
        }
        if (raw.startsWith("ssh://")) {
            URI uri = toUri(raw);
            // SSH 端口与 HTTPS 端口通常不同，转换时只保留域名
            return new RepoUrl("https", uri.getHost(), -1, uri.getPath());
        }
        URI uri = toUri(raw);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new ServiceException("仓库地址仅支持 http/https 协议");
        }
        if (uri.getUserInfo() != null) {
            throw new ServiceException("仓库地址不支持内嵌用户名密码");
        }
        return new RepoUrl(scheme, uri.getHost(), uri.getPort(), uri.getPath());
    }

    private static URI toUri(String raw) {
        try {
            return new URI(raw);
        } catch (URISyntaxException e) {
            throw new ServiceException("仓库地址格式不正确");
        }
    }

    /**
     * 解析项目路径：GitLab 保留多级 group；GitHub/Gitee 只取 owner/repo 两段。
     */
    private static String resolveProjectPath(int repoType, String path) {
        String clean = path == null ? "" : path.replaceAll("^/+", "").replaceAll("/+$", "");
        if (clean.toLowerCase().endsWith(GIT_SUFFIX)) {
            clean = clean.substring(0, clean.length() - GIT_SUFFIX.length());
        }
        String[] segments = clean.split("/");
        if (segments.length < 2) {
            throw new ServiceException("仓库地址格式不正确，需包含项目路径");
        }
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new ServiceException("仓库地址格式不正确");
            }
        }
        if (repoType == REPO_TYPE_GITLAB) {
            return String.join("/", segments);
        }
        return segments[segments.length - 2] + "/" + segments[segments.length - 1];
    }

    private record RepoUrl(String scheme, String host, int port, String path) {
    }
}
