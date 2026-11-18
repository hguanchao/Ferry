package com.ferry.devops.biz.integration.jenkins;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.exception.UtilException;
import com.ferry.common.core.utils.HttpUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.biz.config.DevopsIntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Jenkins 客户端实现：配置化真实 HTTP 下发/中止，未配置时降级日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
class JenkinsClientImpl implements JenkinsClient {

    private final DevopsIntegrationProperties properties;

    @Override
    public void dispatchStage(Long runId, Long stageId, String jobId, String snapshot) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl())) {
            // 降级：未配置 Jenkins 基址时保持原模拟日志
            log.info("Jenkins dispatch (mock): runId={}, stageId={}, jobId={}, snapshotLength={}",
                    runId, stageId, jobId, snapshot == null ? 0 : snapshot.length());
            return;
        }
        String path = StrUtil.isBlank(cfg.getBuildPath()) ? "/job/{job}/buildWithParameters" : cfg.getBuildPath();
        String url = trimTrailingSlash(cfg.getBaseUrl()) + path.replace("{job}", jobId == null ? "" : jobId);
        Map<String, String> headers = authHeaders(cfg);
        Map<String, Object> params = new HashMap<>();
        params.put("runId", runId);
        params.put("stageId", stageId);
        params.put("jobId", jobId);
        params.put("snapshot", snapshot);
        String body = JsonUtil.toJson(params);
        log.info("Jenkins dispatch: runId={}, stageId={}, jobId={}, url={}", runId, stageId, jobId, url);
        try {
            HttpUtil.Response resp = HttpUtil.post(url, body, headers);
            if (!resp.isOk()) {
                throw new ServiceException("Jenkins 下发失败: HTTP " + resp.status());
            }
            // 尝试从响应体记录构建号/队列号；真实 buildNo 回写 run 表由 RunServiceImpl 处理
            Long buildNo = null;
            Long queueId = null;
            try {
                buildNo = JsonUtil.getLong(resp.body(), "buildNo");
                queueId = JsonUtil.getLong(resp.body(), "queueId");
            } catch (UtilException ignore) {
                // Jenkins 成功响应不一定返回 JSON，解析失败不影响下发结果
            }
            log.info("Jenkins dispatch ok: runId={}, stageId={}, status={}, buildNo={}, queueId={}",
                    runId, stageId, resp.status(), buildNo, queueId);
        } catch (UtilException e) {
            log.error("Jenkins 下发失败: runId={}, stageId={}, jobId={}", runId, stageId, jobId, e);
            throw new ServiceException("Jenkins 下发失败: " + e.getMessage());
        }
    }

    @Override
    public void abortBuild(Long runId, Long buildNo) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl())) {
            log.info("Jenkins abort (mock): runId={}, buildNo={}", runId, buildNo);
            return;
        }
        // abortBuild 签名中 jobId 不可得，使用 buildNo 拼 stop 接口
        String url = trimTrailingSlash(cfg.getBaseUrl()) + "/build/" + (buildNo == null ? "" : buildNo) + "/stop";
        Map<String, String> headers = authHeaders(cfg);
        log.info("Jenkins abort: runId={}, buildNo={}, url={}", runId, buildNo, url);
        try {
            HttpUtil.Response resp = HttpUtil.post(url, null, headers);
            if (!resp.isOk()) {
                throw new ServiceException("Jenkins 中止失败: HTTP " + resp.status());
            }
            log.info("Jenkins abort ok: runId={}, buildNo={}, status={}", runId, buildNo, resp.status());
        } catch (UtilException e) {
            log.error("Jenkins 中止失败: runId={}, buildNo={}", runId, buildNo, e);
            throw new ServiceException("Jenkins 中止失败: " + e.getMessage());
        }
    }

    /** user:token 做 Basic 认证头；未配置凭据时不带头（匿名 Jenkins 或仅日志环境） */
    private Map<String, String> authHeaders(DevopsIntegrationProperties.Jenkins cfg) {
        Map<String, String> headers = new HashMap<>();
        if (StrUtil.isNotBlank(cfg.getUser()) && StrUtil.isNotBlank(cfg.getToken())) {
            String basic = Base64.getEncoder().encodeToString(
                    (cfg.getUser() + ":" + cfg.getToken()).getBytes(StandardCharsets.UTF_8));
            headers.put("Authorization", "Basic " + basic);
        }
        return headers;
    }

    private String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
