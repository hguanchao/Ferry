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
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jenkins 客户端实现：一个应用一个 Job，缺失时按模板自动创建；一次运行触发一次构建，
 * 由 Jenkinsfile 内部顺序执行多个 CI 阶段，Ferry 轮询构建/阶段状态。
 *
 * <p>未配置 Jenkins 基址时降级为日志模拟；真实调用使用 Cookie 会话 + CSRF crumb。
 * 下发失败区分“明确失败”和“结果待确认”，由调用方决定运行最终状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
class JenkinsClientImpl implements JenkinsClient {

    private static final Pattern QUEUE_ITEM_PATTERN = Pattern.compile("/queue/item/(\\d+)/?");
    private static final Pattern APP_CODE_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,63}");
    private static final String JOB_TEMPLATE_PATH = "jenkins/ferry-job-template.xml";
    private static final String DEFAULT_BUILD_PATH = "/job/{job}/buildWithParameters";
    private static final int QUEUE_POLLS = 5;
    private static final long QUEUE_POLL_INTERVAL_MS = 300L;

    private final DevopsIntegrationProperties properties;
    private final Set<String> ensuredJobs = ConcurrentHashMap.newKeySet();
    private final Object ensureLock = new Object();
    private volatile JenkinsHttpClient httpClient;
    private volatile String httpClientKey;
    private volatile String jobTemplate;

    @Override
    public JenkinsDispatchResult triggerBuild(Long runId, String snapshot, BuildSource source, String stageSpec) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        String jobName = resolveJobName(source);
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl())) {
            // 降级：未配置 Jenkins 基址时保持模拟日志
            log.info("Jenkins trigger (mock): runId={}, jobName={}, branch={}, commitId={}",
                    runId, jobName, source.branch(), source.commitId());
            return new JenkinsDispatchResult(jobName, null, null);
        }
        JenkinsHttpClient http = httpClient(cfg);
        ensureJob(http, cfg, jobName, source);
        String path = buildPath(cfg, jobName);
        Map<String, String> params = buildParams(runId, snapshot, source, stageSpec, cfg);
        JenkinsHttpClient.HttpResult response = http.postForm(path, params);
        if (response.status() == 404) {
            // Job 可能被外部删除：重建后重试一次
            ensuredJobs.remove(jobName);
            ensureJob(http, cfg, jobName, source);
            response = http.postForm(path, params);
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
    public Long resolveQueueBuild(String appCode, Long queueId) {
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
    public JenkinsBuildStatus buildStatus(String appCode, Long buildNo) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl())) {
            throw new JenkinsDispatchException("Jenkins 未配置，无法查询构建状态", false);
        }
        String jobName = resolveJobName(appCode);
        JenkinsHttpClient http = httpClient(cfg);
        String path = "/job/" + encode(jobName) + "/" + buildNo
                + "/api/json?tree=building,result,timestamp,duration";
        JenkinsHttpClient.HttpResult response = http.get(path);
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

    @Override
    public void abortBuild(Long runId, Long buildNo) {
        DevopsIntegrationProperties.Jenkins cfg = properties.getJenkins();
        if (cfg == null || StrUtil.isBlank(cfg.getBaseUrl())) {
            log.info("Jenkins abort (mock): runId={}, buildNo={}", runId, buildNo);
            return;
        }
        // 运行级 buildNo 尚未接线，保留原有 stop 调用；后续按应用 Job + 阶段构建号再收敛
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

    /**
     * 确保 Job 存在：先查，404 时按模板创建；并发创建冲突则重新确认。
     */
    private void ensureJob(JenkinsHttpClient http, DevopsIntegrationProperties.Jenkins cfg,
                           String jobName, BuildSource source) {
        if (ensuredJobs.contains(jobName)) {
            return;
        }
        synchronized (ensureLock) {
            if (ensuredJobs.contains(jobName)) {
                return;
            }
            String jobPath = "/job/" + encode(jobName) + "/api/json";
            JenkinsHttpClient.HttpResult exists = http.get(jobPath);
            if (exists.status() == 200) {
                // 旧模板创建的 Job 参数不匹配时按新模板更新，避免必须手工删除
                if (cfg.isAutoCreateJob() && managedAndOutdated(http, jobName)) {
                    String xml = renderJobTemplate(jobName, source.appCode());
                    JenkinsHttpClient.HttpResult updated = http.postXml("/job/" + encode(jobName) + "/config.xml", xml);
                    if (!updated.isOk()) {
                        throw new JenkinsDispatchException("更新 Jenkins Job 失败：" + jobName
                                + "，HTTP " + updated.status(), isUncertain(updated.status()));
                    }
                    log.info("Jenkins job updated: {}", jobName);
                }
                ensuredJobs.add(jobName);
                return;
            }
            if (exists.status() != 404) {
                throw new JenkinsDispatchException("检查 Jenkins Job 失败：" + jobName + "，HTTP " + exists.status(),
                        isUncertain(exists.status()));
            }
            if (!cfg.isAutoCreateJob()) {
                throw new JenkinsDispatchException("Jenkins Job 不存在且未开启自动创建：" + jobName, false);
            }
            String xml = renderJobTemplate(jobName, source.appCode());
            JenkinsHttpClient.HttpResult created = http.postXml("/createItem?name=" + encode(jobName), xml);
            if (created.isOk()) {
                ensuredJobs.add(jobName);
                log.info("Jenkins job created: {}", jobName);
                return;
            }
            // 并发创建时可能返回 400/409，重新确认一次即可
            JenkinsHttpClient.HttpResult recheck = http.get(jobPath);
            if (recheck.status() == 200) {
                ensuredJobs.add(jobName);
                return;
            }
            throw new JenkinsDispatchException("创建 Jenkins Job 失败：" + jobName + "，HTTP " + created.status(), false);
        }
    }

    private Map<String, String> buildParams(Long runId, String snapshot, BuildSource source,
                                            String stageSpec, DevopsIntegrationProperties.Jenkins cfg) {
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
        params.put("STAGE_SPEC", stageSpec);
        return params;
    }

    private String buildPath(DevopsIntegrationProperties.Jenkins cfg, String jobName) {
        String template = StrUtil.isBlank(cfg.getBuildPath()) ? DEFAULT_BUILD_PATH : cfg.getBuildPath().trim();
        String path = template.replace("{job}", jobName);
        return path.startsWith("/") ? path : "/" + path;
    }

    /**
     * Job 名直接使用应用编码；编码限制为字母/数字/中划线，避免路径注入。
     */
    private String resolveJobName(BuildSource source) {
        return resolveJobName(source == null ? null : source.appCode());
    }

    private String resolveJobName(String appCode) {
        String code = appCode == null ? "" : appCode.trim();
        if (StrUtil.isBlank(code) || !APP_CODE_PATTERN.matcher(code).matches()) {
            throw new JenkinsDispatchException("应用编码不合法，无法生成 Jenkins Job：" + code, false);
        }
        return code;
    }

    /**
     * 仅更新 Ferry 自动创建的 Job，且只在参数结构不匹配时更新，避免覆盖人工修改。
     *
     * <p>直接读 config.xml 做关键字判断，避免依赖不同 Jenkins 版本的 tree 查询能力。
     */
    private boolean managedAndOutdated(JenkinsHttpClient http, String jobName) {
        JenkinsHttpClient.HttpResult config = http.get("/job/" + encode(jobName) + "/config.xml");
        if (!config.isOk()) {
            log.debug("read Jenkins job config failed: job={}, HTTP={}", jobName, config.status());
            return false;
        }
        String xml = config.body();
        return xml != null
                && xml.contains("Ferry 自动创建")
                && (!xml.contains("STAGE_SPEC")
                || xml.contains("STAGE_ID")
                || xml.contains("CALLBACK_URL"));
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

    private String renderJobTemplate(String jobName, String appCode) {
        return jobTemplate()
                .replace("{{APP_CODE}}", xmlEscape(appCode))
                .replace("{{JOB_NAME}}", xmlEscape(jobName));
    }

    private String jobTemplate() {
        String cached = jobTemplate;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (jobTemplate != null) {
                return jobTemplate;
            }
            try (InputStream input = getClass().getClassLoader().getResourceAsStream(JOB_TEMPLATE_PATH)) {
                if (input == null) {
                    throw new JenkinsDispatchException("缺少 Jenkins Job 模板：" + JOB_TEMPLATE_PATH, false);
                }
                jobTemplate = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                return jobTemplate;
            } catch (IOException e) {
                throw new JenkinsDispatchException("读取 Jenkins Job 模板失败", false, e);
            }
        }
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
            // 基址/凭据变化后本地 Job 存在性缓存不再可信
            ensuredJobs.clear();
            return httpClient;
        }
    }

    private boolean isUncertain(int status) {
        return status >= 500 || status == 408;
    }

    private String xmlEscape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
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
