package com.ferry.devops.biz.integration.repo;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.exception.UtilException;
import com.ferry.common.core.utils.HttpUtil;
import com.ferry.common.core.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 代码平台 Provider 公共能力：HTTP 调用、状态码话术与字段提取。
 */
@Slf4j
abstract class AbstractRepoProvider implements RepoProvider {

    /**
     * 分支候选上限：一次拉取足够覆盖常用分支，避免大仓库分页拖慢弹窗
     */
    protected static final int MAX_BRANCHES = 100;

    /**
     * 发起 GET 请求并解析 JSON。
     *
     * <p>网络异常与响应体解析统一收敛为业务提示，不把三方响应原文透给前端。
     */
    protected JsonNode getJson(String url, Map<String, String> headers, String action) {
        try {
            HttpUtil.Response response = HttpUtil.get(url, headers);
            if (!response.isOk()) {
                log.warn("repo {} failed: HTTP {}", action, response.status());
                throw new ServiceException(statusMessage(response.status(), action));
            }
            return JsonUtil.readTree(response.body());
        } catch (UtilException e) {
            log.error("repo {} request failed", action, e);
            throw new ServiceException("获取" + action + "失败，请检查仓库地址、网络与访问令牌");
        }
    }

    /**
     * 取文本字段，缺失返回空串。
     */
    protected String text(JsonNode node, String field) {
        return nestedText(node, field);
    }

    /**
     * 取嵌套文本字段，如 commit.author.name。
     */
    protected String nestedText(JsonNode node, String... path) {
        JsonNode current = node;
        for (String field : path) {
            current = current.path(field);
        }
        return current.isMissingNode() || current.isNull() ? "" : current.asText();
    }

    /**
     * 提交说明只保留首行，避免多行 message 撑爆下拉项。
     */
    protected String firstLine(String message) {
        if (message == null) {
            return "";
        }
        int lineBreak = message.indexOf('\n');
        return lineBreak < 0 ? message : message.substring(0, lineBreak);
    }

    /**
     * 平台未返回短 SHA 时截取前 8 位。
     */
    protected String shortSha(String sha) {
        return sha != null && sha.length() > 8 ? sha.substring(0, 8) : sha;
    }

    /**
     * URL 编码，用于项目路径与分支名。
     */
    protected String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String statusMessage(int status, String action) {
        return switch (status) {
            case 401, 403 -> "仓库鉴权失败，请检查应用登记的访问令牌";
            case 404 -> "仓库不存在或无权访问，请检查仓库地址与分支";
            case 429 -> "代码平台访问频率超限，请稍后重试";
            default -> "获取" + action + "失败：代码平台返回 HTTP " + status;
        };
    }
}
