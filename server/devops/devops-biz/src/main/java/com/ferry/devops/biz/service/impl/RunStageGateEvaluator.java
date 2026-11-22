package com.ferry.devops.biz.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.RunStageGate;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.biz.config.DevopsProperties;
import com.ferry.devops.biz.gate.GateReportContract;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.RunStageGateMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.service.IRunStageGateService;
import com.ferry.devops.biz.service.IRunStageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * 运行阶段门禁评估器：拉取 Jenkins 归档报告，按单测/扫描规则判定，推进阶段终态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunStageGateEvaluator {

    private static final int GATE_STAGE_UNIT_TEST = 2;
    private static final int GATE_STAGE_STATIC_SCAN = 3;
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    private static final int DEFAULT_EVALUATION_LIMIT = 20;

    private final RunMapper runMapper;
    private final RunStageMapper stageMapper;
    private final AppMapper appMapper;
    private final RunStageGateMapper gateResultMapper;
    private final IRunStageGateService runStageGateService;
    private final IRunStageService runStageService;
    private final JenkinsClient jenkinsClient;
    private final DevopsProperties properties;

    public void evaluatePending() {
        DevopsProperties.Gate cfg = properties.getGate();
        int limit = cfg == null || cfg.getEvaluationLimit() == null
                ? DEFAULT_EVALUATION_LIMIT : cfg.getEvaluationLimit();
        List<RunStage> stages = stageMapper.selectByStatusLimit(StageStatusEnum.PENDING_GATE.getCode(), limit);
        for (RunStage stage : stages) {
            try {
                evaluate(stage.getId());
            } catch (RuntimeException e) {
                log.error("stage gate evaluate failed: runStageId={}", stage.getId(), e);
            }
        }
    }

    public void evaluate(Long runStageId) {
        RunStage stage = stageMapper.selectById(runStageId);
        if (stage == null || !ObjUtil.equals(stage.getStatus(), StageStatusEnum.PENDING_GATE.getCode())
                || !isGateStage(stage.getType())) {
            return;
        }
        Run run = runMapper.selectById(stage.getRunId());
        if (run == null || isTerminalRun(run.getStatus())) {
            return;
        }
        if (!gateEnabled()) {
            writeSummary(stage, "DISABLED", null, null, "阶段门禁未启用");
            runStageService.transitionStage(stage.getId(), StageStatusEnum.SUCCESS, new Date());
            return;
        }
        List<Integer> candidates = ObjUtil.equals(stage.getType(), GATE_STAGE_UNIT_TEST)
                ? GateReportContract.UNIT_TEST_RULE_TYPES : GateReportContract.SCAN_RULE_TYPES;
        List<Integer> applicable = candidates.stream()
                .filter(ruleType -> runStageGateService.hasApplicableRule(
                        run.getAppId(), run.getBranch(), stage.getType(), ruleType))
                .toList();
        if (applicable.isEmpty()) {
            writeSummary(stage, "NOT_APPLICABLE", null, null, null);
            runStageService.transitionStage(stage.getId(), StageStatusEnum.SUCCESS, new Date());
            return;
        }
        App app = appMapper.selectById(run.getAppId());
        String jobName = StrUtil.isNotBlank(run.getJenkinsJobId())
                ? run.getJenkinsJobId() : (app == null ? null : app.getCode());
        if (StrUtil.isBlank(jobName) || run.getJenkinsBuildNo() == null) {
            handleUnavailable(stage, run, applicable, GateReportContract.SCHEMA_VERSION,
                    "Jenkins构建信息缺失，无法拉取门禁报告");
            return;
        }
        String reportPath = ObjUtil.equals(stage.getType(), GATE_STAGE_UNIT_TEST)
                ? GateReportContract.UNIT_TEST_REPORT_PATH : GateReportContract.SCAN_REPORT_PATH;
        Optional<String> report;
        try {
            report = jenkinsClient.fetchArtifact(jobName.trim(), run.getJenkinsBuildNo(), reportPath);
        } catch (RuntimeException e) {
            log.warn("fetch gate report failed: runStageId={}, path={}, reason={}",
                    stage.getId(), reportPath, e.getMessage());
            handleUnavailable(stage, run, applicable, GateReportContract.SCHEMA_VERSION,
                    "门禁报告拉取失败：" + e.getMessage());
            return;
        }
        if (report.isEmpty()) {
            // 归档可能尚未就绪，超时前继续等待
            handleUnavailable(stage, run, applicable, GateReportContract.SCHEMA_VERSION,
                    "门禁报告尚未产出：" + reportPath);
            return;
        }
        ParsedReport parsed = parseReport(stage.getType(), report.get(), applicable);
        for (Integer ruleType : applicable) {
            if (parsed.errors.containsKey(ruleType)) {
                runStageGateService.judgeFetchFailed(run.getId(), stage.getId(), run.getAppId(), run.getBranch(),
                        stage.getType(), ruleType, reportPath, parsed.schemaVersion, parsed.errors.get(ruleType));
            } else {
                runStageGateService.judge(run.getId(), stage.getId(), run.getAppId(), run.getBranch(),
                        stage.getType(), ruleType, parsed.values.get(ruleType), reportPath, parsed.schemaVersion);
            }
        }
        boolean blocked = gateResultMapper.countBlockingByStage(stage.getId()) > 0;
        writeSummary(stage, blocked ? "FAIL" : "PASS", reportPath, parsed.schemaVersion, null);
        runStageService.transitionStage(stage.getId(),
                blocked ? StageStatusEnum.BLOCKED : StageStatusEnum.SUCCESS, new Date());
    }

    private void handleUnavailable(RunStage stage, Run run, List<Integer> applicable,
                                   String schemaVersion, String error) {
        if (!timedOut(stage)) {
            return;
        }
        String reportPath = ObjUtil.equals(stage.getType(), GATE_STAGE_UNIT_TEST)
                ? GateReportContract.UNIT_TEST_REPORT_PATH : GateReportContract.SCAN_REPORT_PATH;
        for (Integer ruleType : applicable) {
            runStageGateService.judgeFetchFailed(run.getId(), stage.getId(), run.getAppId(), run.getBranch(),
                    stage.getType(), ruleType, reportPath, schemaVersion, error);
        }
        writeSummary(stage, "FETCH_FAILED", reportPath, schemaVersion, error);
        runStageService.transitionStage(stage.getId(), StageStatusEnum.BLOCKED, new Date());
    }

    private ParsedReport parseReport(Integer stageType, String body, List<Integer> applicable) {
        Map<Integer, String> values = new LinkedHashMap<>();
        Map<Integer, String> errors = new LinkedHashMap<>();
        JsonNode root;
        try {
            root = JsonUtil.readTree(body);
        } catch (Exception e) {
            return errorAll(applicable, "报告JSON解析失败：" + e.getMessage());
        }
        if (root == null) {
            return errorAll(applicable, "报告内容为空");
        }
        String schemaVersion = root.path("schemaVersion").asText("");
        if (!GateReportContract.SCHEMA_VERSION.equals(schemaVersion)) {
            return errorAll(applicable, "报告schema版本不匹配：" + schemaVersion);
        }
        if (ObjUtil.equals(stageType, GATE_STAGE_UNIT_TEST)) {
            parseUnitTest(root, applicable, values, errors);
        } else {
            parseScan(root, applicable, values, errors);
        }
        return new ParsedReport(values, errors, schemaVersion);
    }

    private void parseUnitTest(JsonNode root, List<Integer> applicable,
                               Map<Integer, String> values, Map<Integer, String> errors) {
        for (Integer ruleType : applicable) {
            switch (ruleType) {
                case 1 -> {
                    Long tests = numeric(root, "tests");
                    Long failures = numeric(root, "failures");
                    Long errorsCount = numeric(root, "errors");
                    if (tests == null || failures == null || errorsCount == null || tests <= 0) {
                        errors.put(ruleType, "单测报告缺少 tests/failures/errors 或 tests<=0");
                    } else {
                        values.put(ruleType, percent(tests - failures - errorsCount, tests));
                    }
                }
                case 2 -> {
                    Long covered = numeric(root, "lineCovered");
                    Long missed = numeric(root, "lineMissed");
                    if (covered == null || missed == null || covered + missed <= 0) {
                        errors.put(ruleType, "单测报告缺少 lineCovered/lineMissed 或分母为0");
                    } else {
                        values.put(ruleType, percent(covered, covered + missed));
                    }
                }
                case 3 -> {
                    Long covered = numeric(root, "branchCovered");
                    Long missed = numeric(root, "branchMissed");
                    if (covered == null || missed == null || covered + missed <= 0) {
                        errors.put(ruleType, "单测报告缺少 branchCovered/branchMissed 或分母为0");
                    } else {
                        values.put(ruleType, percent(covered, covered + missed));
                    }
                }
                default -> errors.put(ruleType, "单测阶段不支持的规则类型：" + ruleType);
            }
        }
    }

    private void parseScan(JsonNode root, List<Integer> applicable,
                           Map<Integer, String> values, Map<Integer, String> errors) {
        JsonNode valuesNode = root.path("values");
        for (Integer ruleType : applicable) {
            JsonNode node = valuesNode.get(String.valueOf(ruleType));
            if (node == null || node.isNull()) {
                errors.put(ruleType, "扫描报告缺少规则类型 " + ruleType + " 的值");
                continue;
            }
            if (ObjUtil.equals(ruleType, 6)) {
                if (node.isArray()) {
                    List<String> licenses = new ArrayList<>();
                    for (JsonNode item : node) {
                        if (!item.isNull()) {
                            licenses.add(item.asText(""));
                        }
                    }
                    values.put(ruleType, String.join(",", licenses));
                } else if (node.isString()) {
                    values.put(ruleType, node.stringValue());
                } else {
                    errors.put(ruleType, "许可证值必须为字符串或数组");
                }
                continue;
            }
            if (node.isNumber()) {
                values.put(ruleType, String.valueOf(node.longValue()));
            } else if (node.isString()) {
                try {
                    Long.parseLong(node.stringValue().trim());
                    values.put(ruleType, node.stringValue().trim());
                } catch (NumberFormatException e) {
                    errors.put(ruleType, "规则类型 " + ruleType + " 的值必须为整数");
                }
            } else {
                errors.put(ruleType, "规则类型 " + ruleType + " 的值必须为数字");
            }
        }
    }

    private ParsedReport errorAll(List<Integer> applicable, String error) {
        Map<Integer, String> errors = new LinkedHashMap<>();
        for (Integer ruleType : applicable) {
            errors.put(ruleType, error);
        }
        return new ParsedReport(new LinkedHashMap<>(), errors, GateReportContract.SCHEMA_VERSION);
    }

    private Long numeric(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.longValue();
        }
        if (node.isString()) {
            try {
                return Long.parseLong(node.stringValue().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String percent(long numerator, long denominator) {
        if (denominator <= 0) {
            return "0.00";
        }
        return String.format(Locale.ROOT, "%.2f", numerator * 100.0 / denominator);
    }

    private void writeSummary(RunStage stage, String gate, String reportPath,
                              String schemaVersion, String error) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("gate", gate);
        if (reportPath != null) {
            summary.put("reportPath", reportPath);
        }
        if (schemaVersion != null) {
            summary.put("schemaVersion", schemaVersion);
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (RunStageGate result : gateResultMapper.listByStage(stage.getId())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ruleType", result.getRuleType());
            item.put("status", result.getStatus());
            item.put("currentValue", result.getCurrentValue());
            item.put("threshold", result.getThreshold());
            results.add(item);
        }
        summary.put("results", results);
        if (StrUtil.isNotBlank(error)) {
            summary.put("error", error);
        }
        stage.setResult(JsonUtil.toJson(summary));
        stageMapper.updateById(stage);
    }

    private boolean timedOut(RunStage stage) {
        DevopsProperties.Gate cfg = properties.getGate();
        int timeout = cfg == null || cfg.getPullTimeoutSeconds() == null
                ? DEFAULT_TIMEOUT_SECONDS : cfg.getPullTimeoutSeconds();
        Date anchor = stage.getEndTime();
        if (anchor == null) {
            anchor = stage.getUpdateTime();
        }
        if (anchor == null) {
            anchor = stage.getCreateTime();
        }
        return anchor != null && anchor.getTime() + timeout * 1000L < System.currentTimeMillis();
    }

    private boolean gateEnabled() {
        return properties.getGate() != null && properties.getGate().isEnabled();
    }

    private boolean isGateStage(Integer stageType) {
        return ObjUtil.equals(stageType, GATE_STAGE_UNIT_TEST)
                || ObjUtil.equals(stageType, GATE_STAGE_STATIC_SCAN);
    }

    private boolean isTerminalRun(Integer status) {
        return ObjUtil.equals(status, RunStatusEnum.SUCCESS.getCode())
                || ObjUtil.equals(status, RunStatusEnum.FAILURE.getCode())
                || ObjUtil.equals(status, RunStatusEnum.TIMEOUT.getCode())
                || ObjUtil.equals(status, RunStatusEnum.CANCELLED.getCode());
    }

    /**
     * 解析后的报告：按规则类型保存值或错误。
     */
    private record ParsedReport(Map<Integer, String> values, Map<Integer, String> errors,
                                String schemaVersion) {
    }
}
