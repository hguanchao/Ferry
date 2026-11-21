package com.ferry.devops.biz.integration.jenkins;

import com.ferry.common.core.exception.UtilException;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.biz.config.DevopsIntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import tools.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jenkins 客户端实现。
 *
 * <p>职责分离：{@link #syncJob} 负责创建/更新 Pipeline Job（读取现有 config.xml 验证类型与归属后
 * 仅改 Ferry 托管字段），{@link #triggerBuild} 只负责以参数触发构建（要求 Job 已同步）。
 * 一次运行触发一次构建，由生成的声明式脚本顺序执行 CI 阶段，Ferry 轮询构建/阶段状态。
 *
 * <p>未配置 Jenkins 基址时降级为日志模拟；真实调用使用 Cookie 会话 + CSRF crumb。
 * 下发失败区分“明确失败”和“结果待确认”，由调用方决定运行最终状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
class JenkinsClientImpl implements JenkinsClient {

    private static final Pattern QUEUE_ITEM_PATTERN = Pattern.compile("/queue/item/(\\d+)/?");
    private static final String DEFAULT_BUILD_PATH = "/job/{job}/buildWithParameters";
    private static final int QUEUE_POLLS = 5;
    private static final long QUEUE_POLL_INTERVAL_MS = 300L;

    private final DevopsIntegrationProperties properties;

    private volatile JenkinsHttpClient httpClient;
    private volatile String httpClientKey;

    /**
     * 创建 Job（幂等）：先查 Job 是否存在，存在直接完成返回。
     */
    @Override
    public void createJob(String jobName, String appCode, String script, boolean disabled,
                          String projectUrl, String projectDisplayName) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl())) {
            log.info("Jenkins createJob (mock): jobName={}, disabled={}", jobName, disabled);
            return;
        }
        JenkinsHttpClient http = httpClient(cfg);
        JenkinsHttpClient.HttpResult existing = http.get(configPath(jobName));
        if (existing.isOk()) {
            // 幂等：Job 已存在时创建动作直接完成，脚本更新交给 updateJob
            log.info("Jenkins job already exists, createJob skipped: jobName={}", jobName);
            return;
        }
        if (existing.status() != 404) {
            throw new JenkinsDispatchException(
                    "读取 Jenkins Job 配置失败：" + jobName + "，HTTP " + existing.status(),
                    isUncertain(existing.status()));
        }
        if (!cfg.isAutoCreateJob()) {
            throw new JenkinsDispatchException("Jenkins Job 不存在且未开启自动创建：" + jobName, false);
        }
        String xml = JenkinsJobXml.buildMinimalJobXml(jobName, appCode, script, disabled,
                projectUrl, projectDisplayName);
        JenkinsHttpClient.HttpResult created =
                http.postXml("/createItem?name=" + encode(jobName), xml);
        if (created.isOk()) {
            log.info("Jenkins job created: jobName={}, disabled={}", jobName, disabled);
            return;
        }
        // 400/409：并发创建或同名冲突，回读确认存在即视为完成
        JenkinsHttpClient.HttpResult confirm = http.get(configPath(jobName));
        if (confirm.isOk()) {
            log.info("Jenkins job create conflicted but confirmed present: jobName={}", jobName);
            return;
        }
        throw new JenkinsDispatchException(
                "创建 Jenkins Job 失败：" + jobName + "，HTTP " + created.status(),
                isUncertain(created.status()));
    }

    /**
     * 更新 Job 配置：先查 Job 是否存在，不存在抛出异常（创建走 {@link #createJob}）。
     */
    @Override
    public void updateJob(String jobName, String appCode, String script, boolean disabled,
                          String projectUrl, String projectDisplayName) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl())) {
            log.info("Jenkins updateJob (mock): jobName={}, disabled={}", jobName, disabled);
            return;
        }
        JenkinsHttpClient http = httpClient(cfg);
        JenkinsHttpClient.HttpResult existing = http.get(configPath(jobName));
        if (existing.status() == 404) {
            throw new JenkinsDispatchException(
                    "Jenkins Job 不存在，请先初始化：" + jobName, false);
        }
        if (!existing.isOk()) {
            throw new JenkinsDispatchException(
                    "读取 Jenkins Job 配置失败：" + jobName + "，HTTP " + existing.status(),
                    isUncertain(existing.status()));
        }
        // 类型与归属校验：非 Pipeline Job 或非 Ferry 管理的同名 Job 一律拒绝覆盖
        Document doc = JenkinsJobXml.loadManagedConfig(existing.body(), jobName);
        if (doc == null) {
            throw new JenkinsDispatchException("Jenkins Job 非 Ferry 管理，拒绝覆盖：" + jobName, false);
        }
        String xml = JenkinsJobXml.applyFerryConfig(doc, script, disabled, projectUrl, projectDisplayName);
        JenkinsHttpClient.HttpResult updated = http.postXml(configPath(jobName), xml);
        if (!updated.isOk()) {
            throw new JenkinsDispatchException(
                    "更新 Jenkins Job 失败：" + jobName + "，HTTP " + updated.status(),
                    isUncertain(updated.status()));
        }
        log.info("Jenkins job updated: jobName={}, disabled={}", jobName, disabled);
    }

    /** Job 配置读取路径（构造 config.xml 地址） */
    private String configPath(String jobName) {
        return "/job/" + encode(jobName) + "/config.xml";
    }

    /**
     * 删除 Job：POST doDelete。404 视为成功（幂等删除）；302/200 为 Jenkins 的删除成功响应；
     * 其余非成功状态按确定性规则抛出。
     */
    @Override
    public void deleteJob(String jobName) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl())) {
            log.info("Jenkins deleteJob (mock): jobName={}", jobName);
            return;
        }
        JenkinsHttpClient http = httpClient(cfg);
        JenkinsHttpClient.HttpResult response =
                http.postForm("/job/" + encode(jobName) + "/doDelete", Map.of());
        if (response.status() == 404 || response.isOk()
                || response.status() == 302 || response.status() == 303) {
            log.info("Jenkins job deleted: jobName={}, status={}", jobName, response.status());
            return;
        }
        throw new JenkinsDispatchException(
                "删除 Jenkins Job 失败：" + jobName + "，HTTP " + response.status(),
                isUncertain(response.status()));
    }

    @Override
    public JenkinsDispatchResult triggerBuild(Long runId, String snapshot, String jobName, BuildSource source) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl())) {
            // 降级：未配置 Jenkins 基址时保持模拟日志
            log.info("Jenkins trigger (mock): runId={}, jobName={}, branch={}, commitId={}",
                    runId, jobName, source.branch(), source.commitId());
            return new JenkinsDispatchResult(jobName, null, null);
        }
        JenkinsHttpClient http = httpClient(cfg);
        String path = buildPath(cfg, jobName);
        Map<String, String> params = buildParams(runId, snapshot, source, cfg);
        JenkinsHttpClient.HttpResult response = http.postForm(path, params);
        if (response.status() == 404) {
            // Job 只由保存同步创建，触发路径不做自动重建
            throw new JenkinsDispatchException("Jenkins Job 不存在或未同步，请先保存流水线触发同步：" + jobName, false);
        }
        if (!response.isOk()) {
            throw new JenkinsDispatchException("Jenkins 触发构建失败：" + jobName + "，HTTP " + response.status(),
                    isUncertain(response.status()));
        }
        Long queueId = parseQueueId(response.header("Location"));
        Long buildNo = queueId == null ? null : pollBuildNo(http, queueId);
        log.info("Jenkins trigger ok: runId={}, jobName={}, queueId={}, buildNo={}",
                runId, jobName, queueId, buildNo);
        return new JenkinsDispatchResult(jobName, queueId, buildNo);
    }

    @Override
    public Long resolveQueueBuild(String jobName, Long queueId) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl()) || queueId == null) {
            return null;
        }
        JenkinsHttpClient http = httpClient(cfg);
        JenkinsHttpClient.HttpResult response = http.get("/queue/item/" + queueId + "/api/json");
        if (!response.isOk()) {
            return null;
        }
        JsonNode root = JsonUtil.readTree(response.body());
        if (root == null) {
            return null;
        }
        JsonNode number = root.at("/executable/number");
        return number.isNumber() ? number.longValue() : null;
    }

    @Override
    public JenkinsBuildStatus buildStatus(String jobName, Long buildNo) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl())) {
            throw new JenkinsDispatchException("Jenkins 未配置，无法查询构建状态", false);
        }
        JenkinsHttpClient http = httpClient(cfg);
        JenkinsHttpClient.HttpResult response = http.get("/job/" + encode(jobName) + "/" + buildNo
                + "/api/json?tree=building,result,timestamp,duration");
        if (!response.isOk()) {
            throw new JenkinsDispatchException("查询 Jenkins 构建失败：" + jobName + " #" + buildNo
                    + "，HTTP " + response.status(), isUncertain(response.status()));
        }
        JsonNode root = JsonUtil.readTree(response.body());
        boolean building = root != null && root.path("building").asBoolean(false);
        String result = null;
        Long timestamp = null;
        Long duration = null;
        if (root != null) {
            JsonNode resultNode = root.path("result");
            if (!resultNode.isMissingNode() && !resultNode.isNull()) {
                String text = resultNode.asText("");
                result = text.isBlank() ? null : text;
            }
            JsonNode timestampNode = root.path("timestamp");
            if (timestampNode.isNumber()) {
                timestamp = timestampNode.longValue();
            }
            JsonNode durationNode = root.path("duration");
            if (durationNode.isNumber()) {
                duration = durationNode.longValue();
            }
        }
        return new JenkinsBuildStatus(buildNo, building, result, timestamp, duration,
                fetchStageStatuses(http, jobName, buildNo));
    }

    @Override
    public void abortBuild(String jobName, Long buildNo) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl())) {
            log.info("Jenkins abort (mock): jobName={}, buildNo={}", jobName, buildNo);
            return;
        }
        if (buildNo == null) {
            // 构建号尚未分配（仍在队列），无可中止对象，运行状态由轮询/取消标记收口
            log.info("Jenkins abort skipped (no buildNo): jobName={}", jobName);
            return;
        }
        JenkinsHttpClient http = httpClient(cfg);
        JenkinsHttpClient.HttpResult response =
                http.postForm("/job/" + encode(jobName) + "/" + buildNo + "/stop", Map.of());
        if (!response.isOk()) {
            throw new JenkinsDispatchException(
                    "Jenkins 中止构建失败：" + jobName + "#" + buildNo + "，HTTP " + response.status(),
                    isUncertain(response.status()));
        }
        log.info("Jenkins abort ok: jobName={}, buildNo={}", jobName, buildNo);
    }

    /**
     * 读取 Pipeline Stage View 的阶段状态；插件不可用时返回空列表，由调用方降级为构建级状态。
     */
    private List<JenkinsStageStatus> fetchStageStatuses(JenkinsHttpClient http, String jobName, Long buildNo) {
        JenkinsHttpClient.HttpResult response = http.get("/job/" + encode(jobName) + "/" + buildNo + "/wfapi/describe");
        if (response.status() == 404 || !response.isOk()) {
            log.debug("Jenkins stage api unavailable: job={}, build={}, HTTP {}",
                    jobName, buildNo, response.status());
            return List.of();
        }
        JsonNode root = JsonUtil.readTree(response.body());
        if (root == null) {
            return List.of();
        }
        List<JenkinsStageStatus> stages = new ArrayList<>();
        for (JsonNode item : root.path("stages")) {
            JsonNode start = item.path("startTimeMillis");
            JsonNode duration = item.path("durationMillis");
            stages.add(new JenkinsStageStatus(
                    item.path("name").asText(""),
                    item.path("status").asText(""),
                    start.isNumber() ? start.longValue() : null,
                    duration.isNumber() ? duration.longValue() : null));
        }
        return stages;
    }

    private Map<String, String> buildParams(Long runId, String snapshot, BuildSource source,
                                            DevopsIntegrationProperties.Jenkins cfg) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("RUN_ID", String.valueOf(runId));
        params.put("APP_CODE", source.appCode());
        params.put("BRANCH", source.branch());
        params.put("COMMIT_ID", source.commitId());
        params.put("REF_TYPE", source.refType() == null ? "1" : String.valueOf(source.refType()));
        params.put("REF_VALUE", source.refValue());
        params.put("REPO_URL", source.repoUrl());
        params.put("SNAPSHOT", snapshot);
        params.put("GIT_CREDENTIALS_ID", cfg.getGitCredentialsId());
        params.put("CALLBACK_URL", source.callbackUrl() == null ? "" : source.callbackUrl());
        params.put("STAGE_MAP", source.stageMap() == null ? "" : source.stageMap());
        return params;
    }

    private String buildPath(DevopsIntegrationProperties.Jenkins cfg, String jobName) {
        String template = StrUtil.isBlank(cfg.getBuildPath()) ? DEFAULT_BUILD_PATH : cfg.getBuildPath().trim();
        String path = template.replace("{job}", jobName);
        return path.startsWith("/") ? path : "/" + path;
    }

    private Long parseQueueId(String location) {
        if (StrUtil.isBlank(location)) {
            return null;
        }
        Matcher matcher = QUEUE_ITEM_PATTERN.matcher(location);
        return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
    }

    /**
     * 短暂轮询队列项，能拿到构建号就回写；拿不到留给回调补齐。
     */
    private Long pollBuildNo(JenkinsHttpClient http, Long queueId) {
        for (int attempt = 0; attempt < QUEUE_POLLS; attempt++) {
            if (attempt > 0) {
                try {
                    Thread.sleep(QUEUE_POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            JenkinsHttpClient.HttpResult response = http.get("/queue/item/" + queueId + "/api/json");
            if (!response.isOk()) {
                continue;
            }
            JsonNode root;
            try {
                root = JsonUtil.readTree(response.body());
            } catch (UtilException e) {
                log.warn("Jenkins queue response parse failed: queueId={}", queueId);
                continue;
            }
            if (root == null) {
                continue;
            }
            JsonNode number = root.at("/executable/number");
            if (number.isNumber()) {
                return number.longValue();
            }
            JsonNode cancelled = root.path("cancelled");
            if (cancelled.isBoolean() && cancelled.booleanValue()) {
                return null;
            }
        }
        return null;
    }

    private JenkinsHttpClient httpClient(DevopsIntegrationProperties.Jenkins cfg) {
        String key = cfg.getBaseUrl() + "|" + cfg.getUser() + "|" + cfg.getToken();
        JenkinsHttpClient current = httpClient;
        if (current != null && key.equals(httpClientKey)) {
            return current;
        }
        synchronized (this) {
            if (httpClient != null && key.equals(httpClientKey)) {
                return httpClient;
            }
            httpClient = new JenkinsHttpClient(cfg.getBaseUrl(), cfg.getUser(), cfg.getToken());
            httpClientKey = key;
            return httpClient;
        }
    }

    private boolean isUncertain(int status) {
        return status >= 500 || status == 408;
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
