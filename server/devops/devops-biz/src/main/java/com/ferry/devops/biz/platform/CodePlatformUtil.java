package com.ferry.devops.biz.platform;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.HttpUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.StrUtil;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 代码平台查询工具
 *
 * <p>覆盖触发链路需要的三个查询：分支列表、分支最近提交、单个提交/标签可达；
 * 三平台接口结构相近，差异用少量分支表达。结果回写（pushCommitStatus/commentOnMR）
 * 与 Jenkins 相关能力不在此处。
 *
 * <p>令牌明文只在内存中短暂使用，禁止落日志；地址打码后再记录。
 */
@Slf4j
public final class CodePlatformUtil {

    private CodePlatformUtil() {
    }

    /**
     * 查询分支列表
     *
     * @param repoType 代码托管平台 GITLAB/GITHUB/GITEE
     * @param repoUrl  仓库 Git 地址
     * @param token    解密后的访问令牌
     * @return 分支名列表
     */
    public static List<String> listBranches(String repoType, String repoUrl, String token) {
        JsonNode root = get(repoType, repoUrl, "/branches?per_page=100", token, "仓库");
        List<String> names = new ArrayList<>();
        for (JsonNode item : root) {
            JsonNode name = item.get("name");
            if (name != null) {
                names.add(name.asString());
            }
        }
        return names;
    }

    /**
     * 查询分支最近提交
     *
     * @param repoType 代码托管平台 GITLAB/GITHUB/GITEE
     * @param repoUrl  仓库 Git 地址
     * @param branch   分支名
     * @param token    解密后的访问令牌
     * @param limit    返回条数上限
     * @return 提交摘要列表，按时间倒序
     */
    public static List<CommitBrief> listCommits(String repoType, String repoUrl, String branch, String token, int limit) {
        String suffix = "/commits?sha=" + encode(branch) + "&per_page=" + limit;
        JsonNode root = get(repoType, repoUrl, suffix, token, "分支");
        List<CommitBrief> briefs = new ArrayList<>();
        for (JsonNode item : root) {
            briefs.add(commitOf(repoType, item));
        }
        return briefs;
    }

    /**
     * 查询单个提交/标签，用于提交可达校验
     *
     * @param repoType 代码托管平台 GITLAB/GITHUB/GITEE
     * @param repoUrl  仓库 Git 地址
     * @param ref      提交 sha 或标签名
     * @param token    解密后的访问令牌
     * @return 提交摘要
     */
    public static CommitBrief getCommit(String repoType, String repoUrl, String ref, String token) {
        JsonNode node = get(repoType, repoUrl, "/commits/" + encode(ref), token, "提交");
        return commitOf(repoType, node);
    }

    /**
     * 发起 GET 并解析 JSON，非 2xx 转业务异常
     *
     * @param repoType     代码托管平台
     * @param repoUrl      仓库 Git 地址
     * @param pathSuffix   仓库级 API 的路径与查询串
     * @param token        访问令牌
     * @param notFoundDesc 404 时的资源描述
     * @return 响应 JSON
     */
    private static JsonNode get(String repoType, String repoUrl, String pathSuffix, String token, String notFoundDesc) {
        String url = apiBase(repoType, repoUrl) + repoPath(repoType, repoUrl) + pathSuffix;
        HttpUtil.Response response;
        try {
            response = HttpUtil.get(url, authHeaders(repoType, token));
        } catch (Exception e) {
            log.warn("代码平台调用失败 {} {}", repoType, mask(url));
            throw new ServiceException("仓库地址不可达或网络异常，请检查平台地址与网络");
        }
        if (!response.isOk()) {
            if (response.status() == 401 || response.status() == 403) {
                throw new ServiceException("平台令牌无效或没有访问权限");
            }
            if (response.status() == 404) {
                throw new ServiceException(notFoundDesc + "不存在，或令牌无权访问");
            }
            throw new ServiceException("代码平台接口调用失败（HTTP " + response.status() + "）");
        }
        return JsonUtil.readTree(response.body());
    }

    /**
     * 平台认证头
     *
     * @param repoType 代码托管平台
     * @param token    访问令牌
     * @return 请求头
     */
    private static Map<String, String> authHeaders(String repoType, String token) {
        return switch (repoType) {
            case "GITLAB" -> Map.of("PRIVATE-TOKEN", token);
            case "GITHUB" -> Map.of("Authorization", "Bearer " + token);
            // Gitee 令牌经请求头传递，避免进入请求地址落日志
            case "GITEE" -> Map.of("access_token", token);
            default -> throw new ServiceException("代码托管平台取值不合法：" + repoType);
        };
    }

    /**
     * 平台 API 基地址
     *
     * <p>GitLab 与 Gitee 的 API 与仓库同域，私有部署天然生效；GitHub 官方走 api.github.com，
     * 企业部署走仓库域名加 /api/v3 前缀。
     *
     * @param repoType 代码托管平台
     * @param repoUrl  仓库 Git 地址
     * @return API 基地址
     */
    private static String apiBase(String repoType, String repoUrl) {
        String host = URI.create(repoUrl).getHost();
        return switch (repoType) {
            case "GITLAB", "GITEE" -> "https://" + host;
            case "GITHUB" -> "github.com".equalsIgnoreCase(host) ? "https://api.github.com" : "https://" + host + "/api/v3";
            default -> throw new ServiceException("代码托管平台取值不合法：" + repoType);
        };
    }

    /**
     * 平台侧仓库路径
     *
     * <p>GitLab 支持子组，整体 URL-encode 作为 project id；GitHub/Gitee 为 owner/repo。
     *
     * @param repoType 代码托管平台
     * @param repoUrl  仓库 Git 地址
     * @return 编码后的仓库路径
     */
    private static String repoPath(String repoType, String repoUrl) {
        URI uri = URI.create(repoUrl);
        String path = uri.getPath();
        if (path == null || path.length() < 2) {
            throw new ServiceException("仓库地址缺少仓库路径");
        }
        String repoPath = path.substring(1);
        if (repoPath.endsWith(".git")) {
            repoPath = repoPath.substring(0, repoPath.length() - 4);
        }
        if ("GITLAB".equals(repoType)) {
            return "/" + URLEncoder.encode(repoPath, StandardCharsets.UTF_8);
        }
        return "/" + repoPath;
    }

    /**
     * 单条提交记录转摘要
     *
     * <p>GitLab 字段平铺在节点上；GitHub/Gitee 的提交人、时间与说明嵌套在 commit 对象里。
     *
     * @param repoType 代码托管平台
     * @param node     提交节点
     * @return 提交摘要
     */
    private static CommitBrief commitOf(String repoType, JsonNode node) {
        if ("GITLAB".equals(repoType)) {
            return new CommitBrief(text(node, "id"), text(node, "author_name"),
                    parseTime(text(node, "committed_date")), text(node, "title"));
        }
        JsonNode commit = node.get("commit");
        JsonNode author = commit == null ? null : commit.get("author");
        return new CommitBrief(text(node, "sha"),
                author == null ? "" : text(author, "name"),
                parseTime(author == null ? null : text(author, "date")),
                commit == null ? "" : text(commit, "message"));
    }

    /**
     * 读取对象字段的文本值
     *
     * @param node      对象节点
     * @param fieldName 字段名
     * @return 文本值，缺失返回空串
     */
    private static String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null ? "" : value.asString();
    }

    /**
     * 解析平台返回的 ISO-8601 时间
     *
     * @param text 时间文本
     * @return 时间，解析失败返回 null
     */
    private static Date parseTime(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return Date.from(OffsetDateTime.parse(text).toInstant());
        } catch (Exception e) {
            // 平台时间格式异常不阻断主流程，提交时间允许为空
            return null;
        }
    }

    /**
     * 编码路径片段
     *
     * @param value 原文
     * @return URL 编码后的文本
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 日志里的地址打码：剥离查询串，避免令牌类参数进入日志
     *
     * @param url 请求地址
     * @return 打码后的地址
     */
    private static String mask(String url) {
        int index = url.indexOf('?');
        return index > 0 ? url.substring(0, index) + "?..." : url;
    }
}
