package com.ferry.devops.biz.integration.jenkins;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.exception.UtilException;
import com.ferry.common.core.http.R;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.HttpUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.api.entity.GateResult;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.entity.RunTask;
import com.ferry.devops.api.enums.BlockReasonEnum;
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.mapper.RunTaskMapper;
import com.ferry.devops.biz.service.IGateResultService;
import com.ferry.devops.biz.service.IRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Jenkins 结果回传回调（公开路径，HMAC-SHA256 验签）。
 *
 * <p>签名：以请求参数按字典序拼成 {@code k=v&...} 作为待签串，HMAC-SHA256(secret, 待签串) 转小写十六进制，
 * 放在请求头 {@code X-Ferry-Signature}。secret 未配置时跳过验签（本地/试点环境）；配置后校验不通过返回 401 语义失败。
 * 状态以本系统为准；Ferry 生成的 Job 通过脚本内 {@code ferryNotify} 主动上报，本通道为主通道，
 * {@code RunPollingScheduler} 降频轮询兜底对账；结果待确认不直接置成功。
 * 约定 {@code stageId=0} 为构建前置失败（Checkout 等 Ferry 阶段之前的步骤），直接收敛运行终态。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/callback/jenkins")
@Tag(name = "Jenkins回调", description = "Jenkins 结果回传入站接口")
public class JenkinsWebhookController {

    /** 参与 Jenkins 执行的 CI 阶段类型上限：1构建 2单测 3静态扫描 4制品入库；5-8 属部署/审批域，由 Ferry 侧执行器推进 */
    private static final int CI_STAGE_TYPE_MAX = 4;

    /** 任务状态镜像码（与轮询 syncTaskStatus 同口径）：3成功 4失败 6跳过 9已取消 */
    private static final int TASK_STATUS_SUCCESS = 3;
    private static final int TASK_STATUS_FAILURE = 4;
    private static final int TASK_STATUS_SKIPPED = 6;
    private static final int TASK_STATUS_CANCELLED = 9;

    private final RunMapper runMapper;
    private final RunStageMapper stageMapper;
    private final RunTaskMapper taskMapper;
    private final IRunService runService;
    private final IGateResultService gateResultService;

    @Value("${ferry.devops.jenkins.callback-secret:}")
    private String callbackSecret;

    /** reportType -> ruleType：1单测通过率 2行覆盖率 3分支覆盖率 4阻断漏洞 5高危依赖 6许可证黑名单 */
    private static final Map<String, Integer> REPORT_RULE_TYPE = Map.ofEntries(
            Map.entry("UNIT_TEST", 1), Map.entry("UNIT_TEST_PASS_RATE", 1), Map.entry("JUNIT", 1), Map.entry("unit_test", 1),
            Map.entry("LINE_COVERAGE", 2), Map.entry("COVERAGE", 2), Map.entry("line_coverage", 2), Map.entry("coverage", 2),
            Map.entry("BRANCH_COVERAGE", 3), Map.entry("branch_coverage", 3),
            Map.entry("BLOCKING_VULN", 4), Map.entry("SECURITY_SCAN", 4), Map.entry("static_scan", 4), Map.entry("STATIC_SCAN", 4),
            Map.entry("HIGH_RISK_DEP", 5), Map.entry("SCA", 5), Map.entry("DEPENDENCY", 5), Map.entry("high_risk_dependency", 5),
            Map.entry("LICENSE_BLACKLIST", 6), Map.entry("LICENSE", 6), Map.entry("license_blacklist", 6));

    /**
     * 状态回调：runId + stageId + buildNo + status
     */
    @PostMapping("/status")
    @Operation(summary = "Jenkins状态回调")
    public R<String> status(@RequestParam("runId") Long runId,
                            @RequestParam("stageId") Long stageId,
                            @RequestParam(value = "buildNo", required = false) Long buildNo,
                            @RequestParam("status") String status,
                            @RequestParam(value = "exitCode", required = false) Integer exitCode,
                            @RequestParam(value = "signature", required = false) String signature,
                            jakarta.servlet.http.HttpServletRequest request) {
        if (!verify(runId, stageId, buildNo, status, signature, request)) {
            return R.fail(401, "回调签名校验失败");
        }
        Run run = runMapper.selectById(runId);
        if (run == null) {
            throw new ServiceException("回调对应的运行不存在");
        }
        // 约定 stageId=0 表示构建前置失败（Checkout 等 Ferry 阶段之前的步骤），无对应运行阶段
        if (Long.valueOf(0L).equals(stageId)) {
            return buildLevelCallback(run, status);
        }
        RunStage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            throw new ServiceException("回调对应的阶段不存在");
        }
        RunStatusEnum runStatus = parseRunStatus(status);
        StageStatusEnum stageStatus = parseStageStatus(status);
        if (stageStatus == null) {
            // 非阶段级状态：保持原有运行级回传能力
            if (runStatus != null) {
                run.setStatus(runStatus.getCode());
                if (isTerminalRunStatus(runStatus.getCode())) {
                    run.setEndTime(new Date());
                    if (run.getStartTime() != null) {
                        run.setDuration(run.getEndTime().getTime() - run.getStartTime().getTime());
                    }
                }
                runMapper.updateById(run);
            }
            return R.success("回调已接收");
        }
        // 重复回调幂等：阶段已终态时不再处理，避免重复推进下一阶段
        if (isTerminalStageStatus(stage.getStatus())) {
            log.info("ignore terminal stage callback: runId={}, stageId={}, previous={}, incoming={}",
                    runId, stageId, stage.getStatus(), stageStatus);
            return R.success("回调已接收");
        }
        Date now = new Date();
        stage.setStatus(stageStatus.getCode());
        if (stageStatus == StageStatusEnum.RUNNING) {
            if (stage.getStartTime() == null) {
                stage.setStartTime(now);
            }
            stageMapper.updateById(stage);
            if (run.getStartTime() == null) {
                run.setStartTime(now);
            }
            if (!isTerminalRunStatus(run.getStatus())) {
                run.setStatus(RunStatusEnum.RUNNING.getCode());
                runMapper.updateById(run);
            }
            return R.success("回调已接收");
        }
        stage.setEndTime(now);
        if (stage.getStartTime() != null) {
            stage.setDuration(now.getTime() - stage.getStartTime().getTime());
        }
        stageMapper.updateById(stage);
        mirrorTasks(stage, stageStatus);
        if (stageStatus == StageStatusEnum.SUCCESS) {
            if (run.getStartTime() == null) {
                run.setStartTime(now);
            }
            if (allCiSettled(runId)) {
                // webhook 为主通道：CI 阶段全部终态时立即收敛运行终态，不等轮询兜底；
                // 已有终态（如先行阶段失败）不覆盖，后面再到的迟到 SUCCESS 只更新阶段本身
                if (!isTerminalRunStatus(run.getStatus())) {
                    run.setStatus(RunStatusEnum.SUCCESS.getCode());
                    run.setEndTime(now);
                    if (run.getStartTime() != null) {
                        run.setDuration(run.getEndTime().getTime() - run.getStartTime().getTime());
                    }
                    skipNonCiNotStarted(runId);
                }
            } else if (!isTerminalRunStatus(run.getStatus())) {
                run.setStatus(RunStatusEnum.RUNNING.getCode());
            }
            runMapper.updateById(run);
            return R.success("回调已接收");
        }
        // 阶段失败/超时/取消：后续未开始阶段全部跳过，运行进入对应终态
        for (RunStage later : stageMapper.selectByRun(runId)) {
            if (later.getSeq() != null && stage.getSeq() != null && later.getSeq() > stage.getSeq()
                    && ObjUtil.equals(later.getStatus(), StageStatusEnum.NOT_STARTED.getCode())) {
                later.setStatus(StageStatusEnum.SKIPPED.getCode());
                stageMapper.updateById(later);
                mirrorTasks(later, StageStatusEnum.SKIPPED);
            }
        }
        RunStatusEnum terminalRunStatus = switch (stageStatus) {
            case TIMEOUT -> RunStatusEnum.TIMEOUT;
            case CANCELLED -> RunStatusEnum.CANCELLED;
            default -> RunStatusEnum.FAILURE;
        };
        run.setStatus(terminalRunStatus.getCode());
        run.setEndTime(now);
        if (run.getStartTime() != null) {
            run.setDuration(now.getTime() - run.getStartTime().getTime());
        }
        runMapper.updateById(run);
        return R.success("回调已接收");
    }

    /**
     * 报告/摘要回调：拉取报告并触发门禁判定，汇总阻塞时置运行为阻塞。
     *
     * <p>reportType 无法映射或 objectKey 为空时保持原 log 降级；报告拉取/schema 失败写 FETCH_FAILED（不可豁免且阻塞）。
     */
    @PostMapping("/report")
    @Operation(summary = "Jenkins报告摘要回调")
    public R<String> report(@RequestParam("runId") Long runId,
                            @RequestParam("stageId") Long stageId,
                            @RequestParam(value = "reportType", required = false) String reportType,
                            @RequestParam(value = "objectKey", required = false) String objectKey,
                            @RequestParam(value = "signature", required = false) String signature,
                            jakarta.servlet.http.HttpServletRequest request) {
        if (!verify(runId, stageId, null, reportType, signature, request)) {
            return R.fail(401, "回调签名校验失败");
        }
        log.info("Jenkins report callback: runId={}, stageId={}, type={}, objectKey={}", runId, stageId, reportType, objectKey);
        Integer ruleType = ruleTypeOf(reportType);
        if (ruleType == null || StrUtil.isBlank(objectKey)) {
            // 降级：无法映射规则类型或报告地址为空时不参与门禁判定，保持原 log 行为
            log.info("Jenkins report skip gate judge: runId={}, stageId={}, type={}, objectKey={}",
                    runId, stageId, reportType, objectKey);
            return R.success("报告回调已接收");
        }
        String schemaVersion = StrUtil.defaultIfBlank(request.getParameter("schemaVersion"), "1.0");
        String source = objectKey;
        try {
            Run run = runMapper.selectById(runId);
            if (run == null) {
                log.warn("Jenkins report run not found: runId={}", runId);
                return R.success("报告回调已接收");
            }
            Long appId = run.getAppId();
            String branch = run.getBranch();
            // 解析失败标志（附录 D：parseStatus=FAILED 带 error 时判数据获取失败）
            String parseStatus = request.getParameter("parseStatus");
            if ("FAILED".equalsIgnoreCase(parseStatus)) {
                String error = StrUtil.defaultIfBlank(request.getParameter("error"), "报告解析失败");
                GateResult result = gateResultService.judgeFetchFailed(
                        runId, stageId, appId, branch, ruleType, source, schemaVersion, error);
                blockIfNeeded(runId, result.getConclusion());
                return R.success("报告回调已接收");
            }
            String currentValue;
            if (isFullUrl(objectKey)) {
                HttpUtil.Response resp;
                try {
                    resp = HttpUtil.get(objectKey, null);
                } catch (UtilException e) {
                    log.error("Jenkins 报告拉取失败: runId={}, stageId={}, objectKey={}", runId, stageId, objectKey, e);
                    GateResult result = gateResultService.judgeFetchFailed(runId, stageId, appId, branch,
                            ruleType, source, schemaVersion, "报告拉取失败: " + e.getMessage());
                    blockIfNeeded(runId, result.getConclusion());
                    return R.success("报告回调已接收");
                }
                if (!resp.isOk()) {
                    log.warn("Jenkins 报告拉取非成功: runId={}, stageId={}, status={}", runId, stageId, resp.status());
                    GateResult result = gateResultService.judgeFetchFailed(runId, stageId, appId, branch,
                            ruleType, source, schemaVersion, "报告拉取失败: HTTP " + resp.status());
                    blockIfNeeded(runId, result.getConclusion());
                    return R.success("报告回调已接收");
                }
                currentValue = extractReportValue(resp.body(), ruleType);
            } else {
                // 非完整地址无法拉取，按规则类型取默认值判定
                currentValue = defaultValue(ruleType);
            }
            GateResult result = gateResultService.judge(runId, stageId, appId, branch, ruleType,
                    currentValue, source, schemaVersion);
            blockIfNeeded(runId, result.getConclusion());
        } catch (ServiceException e) {
            log.error("Jenkins 报告门禁判定失败: runId={}, stageId={}", runId, stageId, e);
        } catch (Exception e) {
            log.error("Jenkins 报告回调处理异常: runId={}, stageId={}", runId, stageId, e);
        }
        return R.success("报告回调已接收");
    }

    /** reportType -> ruleType；无法映射返回 null 走降级。 */
    private Integer ruleTypeOf(String reportType) {
        if (StrUtil.isBlank(reportType)) {
            return null;
        }
        return REPORT_RULE_TYPE.get(reportType.trim().toUpperCase());
    }

    /** 报告体内取值：passRate / coverage / value 依次尝试，取不到按规则类型默认。 */
    private String extractReportValue(String body, Integer ruleType) {
        if (StrUtil.isNotBlank(body)) {
            try {
                JsonNode root = JsonUtil.readTree(body);
                if (root != null) {
                    for (String field : new String[]{"passRate", "coverage", "value"}) {
                        JsonNode node = root.get(field);
                        if (node == null || node.isNull()) {
                            continue;
                        }
                        if (node.isString()) {
                            return node.stringValue();
                        }
                        if (node.isNumber()) {
                            return String.valueOf(node.doubleValue());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Jenkins 报告体解析失败，按默认值判定: ruleType={}", ruleType, e);
            }
        }
        return defaultValue(ruleType);
    }

    /** 各规则类型默认值：比率类 100，计数/枚举类 0。 */
    private String defaultValue(Integer ruleType) {
        return ruleType != null && ruleType <= 3 ? "100" : "0";
    }

    private boolean isFullUrl(String s) {
        return s != null && (s.startsWith("http://") || s.startsWith("https://"));
    }

    /** 汇总门禁阻塞时置运行为阻塞，并按结论区分阻塞原因（FETCH_FAILED=2，其余=1）。 */
    private void blockIfNeeded(Long runId, Integer conclusion) {
        try {
            if (!gateResultService.isRunBlocked(runId)) {
                return;
            }
            Run run = runMapper.selectById(runId);
            if (run == null || ObjUtil.equals(run.getStatus(), RunStatusEnum.BLOCKED.getCode())) {
                return;
            }
            run.setStatus(RunStatusEnum.BLOCKED.getCode());
            boolean fetchFailed = conclusion != null
                    && ObjUtil.equals(conclusion, com.ferry.devops.api.enums.GateConclusionEnum.FETCH_FAILED.getCode());
            run.setBlockReason(fetchFailed ? BlockReasonEnum.GATE_FETCH_FAILED.getCode()
                    : BlockReasonEnum.GATE_FAILED.getCode());
            runMapper.updateById(run);
        } catch (Exception e) {
            log.error("Jenkins 报告阻塞运行失败: runId={}", runId, e);
        }
    }


    /**
     * 日志回调：接收 Jenkins 原文，入库前脱敏并分段。
     */
    @PostMapping("/log")
    @Operation(summary = "Jenkins日志回调")
    public R<String> log(@RequestParam("runId") Long runId,
                         @RequestParam("taskId") Long taskId,
                         @RequestParam(value = "signature", required = false) String signature,
                         @RequestParam("content") String content,
                         jakarta.servlet.http.HttpServletRequest request) {
        if (!verify(runId, taskId, null, "LOG", signature, request)) {
            return R.fail(401, "回调签名校验失败");
        }
        runService.saveLog(runId, taskId, content);
        return R.success("日志已接收");
    }

    /**
     * 构建前置失败回调：Checkout 等前置步骤失败时无对应运行阶段，直接收敛运行终态。
     *
     * <p>仅终态（失败/超时/已取消）有意义；运行态/成功对前置阶段无意义，忽略不处理。
     * 构建未进入任何 Ferry 阶段，全部未开始阶段（含 CI）置跳过。
     */
    private R<String> buildLevelCallback(Run run, String status) {
        RunStatusEnum runStatus = parseRunStatus(status);
        if (runStatus == null || runStatus == RunStatusEnum.RUNNING || runStatus == RunStatusEnum.SUCCESS) {
            return R.success("回调已接收");
        }
        if (isTerminalRunStatus(run.getStatus())) {
            return R.success("回调已接收");
        }
        RunStatusEnum terminal = switch (runStatus) {
            case TIMEOUT -> RunStatusEnum.TIMEOUT;
            case CANCELLED -> RunStatusEnum.CANCELLED;
            default -> RunStatusEnum.FAILURE;
        };
        Date now = new Date();
        run.setStatus(terminal.getCode());
        run.setEndTime(now);
        if (run.getStartTime() != null) {
            run.setDuration(run.getEndTime().getTime() - run.getStartTime().getTime());
        }
        runMapper.updateById(run);
        for (RunStage stage : stageMapper.selectByRun(run.getId())) {
            if (ObjUtil.equals(stage.getStatus(), StageStatusEnum.NOT_STARTED.getCode())) {
                stage.setStatus(StageStatusEnum.SKIPPED.getCode());
                stageMapper.updateById(stage);
                mirrorTasks(stage, StageStatusEnum.SKIPPED);
            }
        }
        return R.success("回调已接收");
    }

    /** CI 阶段（type≤4）是否已全部进入终态；REUSED 复用阶段按终态计，断点重跑不受影响 */
    private boolean allCiSettled(Long runId) {
        return stageMapper.selectByRun(runId).stream()
                .filter(stage -> stage.getStageType() != null && stage.getStageType() <= CI_STAGE_TYPE_MAX)
                .allMatch(stage -> isTerminalStageStatus(stage.getStatus()));
    }

    /** 部署/发布域未开始阶段置跳过，与轮询 markNotStartedSkipped 口径一致 */
    private void skipNonCiNotStarted(Long runId) {
        for (RunStage stage : stageMapper.selectByRun(runId)) {
            if (stage.getStageType() != null && stage.getStageType() > CI_STAGE_TYPE_MAX
                    && ObjUtil.equals(stage.getStatus(), StageStatusEnum.NOT_STARTED.getCode())) {
                stage.setStatus(StageStatusEnum.SKIPPED.getCode());
                stageMapper.updateById(stage);
                mirrorTasks(stage, StageStatusEnum.SKIPPED);
            }
        }
    }

    /**
     * 任务状态镜像阶段状态（与轮询 syncTaskStatus 同口径）：Jenkins 阶段级结果无法精确到任务，
     * 先保证页面状态一致；仅在状态变化时更新，回调与轮询双通道写入天然幂等。
     */
    private void mirrorTasks(RunStage stage, StageStatusEnum stageStatus) {
        Integer taskStatus = switch (stageStatus) {
            case SUCCESS -> TASK_STATUS_SUCCESS;
            case FAILURE -> TASK_STATUS_FAILURE;
            case SKIPPED -> TASK_STATUS_SKIPPED;
            case CANCELLED -> TASK_STATUS_CANCELLED;
            default -> null;
        };
        if (taskStatus == null) {
            return;
        }
        List<RunTask> tasks = taskMapper.selectByStage(stage.getId());
        if (CollUtil.isEmpty(tasks)) {
            return;
        }
        for (RunTask task : tasks) {
            if (!ObjUtil.equals(task.getStatus(), taskStatus)) {
                task.setStatus(taskStatus);
                taskMapper.updateById(task);
            }
        }
    }

    /**
     * 验签：secret 为空时跳过；否则优先取参数签名，再取请求头签名。
     */
    private boolean verify(Object... parts) {
        if (callbackSecret == null || callbackSecret.isBlank()) {
            return true;
        }
        Long runId = (Long) parts[0];
        Long stageId = (Long) parts[1];
        Long buildNo = (Long) parts[2];
        String status = (String) parts[3];
        String signature = (String) parts[4];
        jakarta.servlet.http.HttpServletRequest request = (jakarta.servlet.http.HttpServletRequest) parts[5];
        String provided = signature != null ? signature : request.getHeader("X-Ferry-Signature");
        if (provided == null || provided.isBlank()) {
            return false;
        }
        String canonical = "runId=" + runId + "&stageId=" + stageId
                + "&buildNo=" + (buildNo == null ? "" : buildNo) + "&status=" + status;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(callbackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
            return expected.equalsIgnoreCase(provided);
        } catch (Exception e) {
            log.warn("Jenkins callback sign error", e);
            return false;
        }
    }

    private boolean isTerminalStageStatus(Integer status) {
        return ObjUtil.equals(status, StageStatusEnum.SUCCESS.getCode())
                || ObjUtil.equals(status, StageStatusEnum.FAILURE.getCode())
                || ObjUtil.equals(status, StageStatusEnum.SKIPPED.getCode())
                || ObjUtil.equals(status, StageStatusEnum.TIMEOUT.getCode())
                || ObjUtil.equals(status, StageStatusEnum.REUSED.getCode())
                || ObjUtil.equals(status, StageStatusEnum.CANCELLED.getCode());
    }

    private boolean isTerminalRunStatus(Integer status) {
        return ObjUtil.equals(status, RunStatusEnum.SUCCESS.getCode())
                || ObjUtil.equals(status, RunStatusEnum.FAILURE.getCode())
                || ObjUtil.equals(status, RunStatusEnum.TIMEOUT.getCode())
                || ObjUtil.equals(status, RunStatusEnum.CANCELLED.getCode());
    }

    private RunStatusEnum parseRunStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "RUNNING", "STARTED" -> RunStatusEnum.RUNNING;
            case "SUCCESS" -> RunStatusEnum.SUCCESS;
            case "FAILURE", "ERROR" -> RunStatusEnum.FAILURE;
            case "TIMEOUT", "ABORTED" -> RunStatusEnum.TIMEOUT;
            case "CANCELLED" -> RunStatusEnum.CANCELLED;
            default -> null;
        };
    }

    private StageStatusEnum parseStageStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "RUNNING", "STARTED" -> StageStatusEnum.RUNNING;
            case "SUCCESS" -> StageStatusEnum.SUCCESS;
            case "FAILURE", "ERROR" -> StageStatusEnum.FAILURE;
            case "TIMEOUT", "ABORTED" -> StageStatusEnum.TIMEOUT;
            case "CANCELLED" -> StageStatusEnum.CANCELLED;
            default -> null;
        };
    }
}
