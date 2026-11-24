package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import tools.jackson.databind.JsonNode;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.AiFeedbackDTO;
import com.ferry.devops.api.dto.AppAiLevelDTO;
import com.ferry.devops.api.dto.query.AiFeedbackQuery;
import com.ferry.devops.api.dto.query.AiOutputQuery;
import com.ferry.devops.api.entity.AiEvalSample;
import com.ferry.devops.api.entity.AiEvidence;
import com.ferry.devops.api.entity.AiFeedback;
import com.ferry.devops.api.entity.AiOutput;
import com.ferry.devops.api.entity.AppAiLevel;
import com.ferry.devops.api.entity.Artifact;
import com.ferry.devops.api.entity.RunStageGate;
import com.ferry.devops.api.entity.ReleaseArtifact;
import com.ferry.devops.api.entity.ReleaseCandidate;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.entity.RunStageTask;
import com.ferry.devops.api.enums.AiConfidenceEnum;
import com.ferry.devops.api.enums.AiOutputStatusEnum;
import com.ferry.devops.api.enums.AiOutputTypeEnum;
import com.ferry.devops.api.vo.AiEvidenceVO;
import com.ferry.devops.api.vo.AiFeedbackVO;
import com.ferry.devops.api.vo.AiOutputVO;
import com.ferry.devops.api.vo.AiQualityVO;
import com.ferry.devops.api.vo.AppAiLevelVO;
import com.ferry.common.ai.utils.AiUtil;
import com.ferry.devops.biz.mapper.AiEvalSampleMapper;
import com.ferry.devops.biz.mapper.AiEvidenceMapper;
import com.ferry.devops.biz.mapper.AiFeedbackMapper;
import com.ferry.devops.biz.mapper.AiOutputMapper;
import com.ferry.devops.biz.mapper.AppAiLevelMapper;
import com.ferry.devops.biz.mapper.ArtifactMapper;
import com.ferry.devops.biz.mapper.RunStageGateMapper;
import com.ferry.devops.biz.mapper.ReleaseArtifactMapper;
import com.ferry.devops.biz.mapper.ReleaseCandidateMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.mapper.RunStageTaskMapper;
import com.ferry.devops.biz.service.IAiService;
import com.ferry.devops.biz.wrapper.AiOutputWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 归因、反馈与质量服务实现
 */
@Service
@RequiredArgsConstructor
public class AiServiceImpl extends ServiceImpl<AiOutputMapper, AiOutput> implements IAiService {

    /** 模型不可用时的业务兜底结论 */
    private static final String FALLBACK_CONCLUSION = "基于证据分析：结论待确认";

    private final AiEvidenceMapper evidenceMapper;
    private final AiFeedbackMapper feedbackMapper;
    private final AiEvalSampleMapper evalSampleMapper;
    private final AppAiLevelMapper appAiLevelMapper;
    // 真实证据链依赖：四个入口据此从库里取证据，analyze 里逐条回查校验
    private final RunMapper runMapper;
    private final RunStageMapper stageMapper;
    private final RunStageTaskMapper taskMapper;
    private final ReleaseCandidateMapper releaseCandidateMapper;
    private final ReleaseArtifactMapper releaseArtifactMapper;
    private final ArtifactMapper artifactMapper;
    private final RunStageGateMapper gateResultMapper;

    @Override
    public AiOutputVO attribution(Long runId) {
        // 归因对象是流水线本次运行：证据取自各阶段/任务的执行结果，而非日志正文
        List<String> evidence = new ArrayList<>();
        List<RunStage> stages = stageMapper.selectByRun(runId);
        if (CollUtil.isNotEmpty(stages)) {
            for (RunStage stage : stages) {
                if (StrUtil.isNotBlank(stage.getResult())) {
                    evidence.add("stage:" + stage.getId());
                }
            }
        }
        List<RunStageTask> tasks = taskMapper.selectByRun(runId);
        if (CollUtil.isNotEmpty(tasks)) {
            for (RunStageTask task : tasks) {
                if (StrUtil.isNotBlank(task.getResult())) {
                    evidence.add("task:" + task.getId());
                }
            }
        }
        Run run = runMapper.selectById(runId);
        if (run != null && StrUtil.isNotBlank(run.getRefValue())) {
            evidence.add("commit:" + run.getRefValue());
        }
        return analyze(AiOutputTypeEnum.FAILURE_ATTRIBUTION, String.valueOf(runId), null,
                run == null ? null : run.getAppId(), null, runId, evidence);
    }

    @Override
    public AiOutputVO admission(Long rcId) {
        return analyzeRc(AiOutputTypeEnum.ADMISSION, rcId);
    }

    @Override
    public AiOutputVO plan(Long rcId) {
        return analyzeRc(AiOutputTypeEnum.RELEASE_PLAN, rcId);
    }

    /**
     * 准入辅助 / 发布预案共用：发布单描述与风险明细 + 关联运行的门禁结论。
     */
    private AiOutputVO analyzeRc(AiOutputTypeEnum type, Long rcId) {
        List<String> evidence = new ArrayList<>();
        ReleaseCandidate rc = releaseCandidateMapper.selectById(rcId);
        if (rc != null) {
            if (StrUtil.isNotBlank(rc.getDescription())) {
                evidence.add("rc:" + rcId + ":desc:" + oneLine(rc.getDescription()));
            }
            if (StrUtil.isNotBlank(rc.getRiskDetail())) {
                evidence.add("rc:" + rcId + ":risk:" + oneLine(rc.getRiskDetail()));
            }
        }
        // 发布单 -> 发布制品 -> 制品产出运行 -> 门禁结果
        List<Map<String, Object>> artifacts = releaseArtifactMapper.selectByRc(rcId);
        Set<Long> runIds = new LinkedHashSet<>();
        if (CollUtil.isNotEmpty(artifacts)) {
            for (Map<String, Object> item : artifacts) {
                Object artifactId = item.get("artifactId");
                if (artifactId == null) {
                    continue;
                }
                Artifact artifact = artifactMapper.selectById(((Number) artifactId).longValue());
                if (artifact != null && artifact.getRunId() != null) {
                    runIds.add(artifact.getRunId());
                }
            }
        }
        if (!runIds.isEmpty()) {
            List<RunStageGate> gates = gateResultMapper.selectList(
                    Wrappers.<RunStageGate>lambdaQuery().in(RunStageGate::getRunId, runIds));
            for (RunStageGate gate : gates) {
                evidence.add("gate:" + gate.getId() + ":" + gateConclusionText(gate.getStatus()));
            }
        }
        return analyze(type, String.valueOf(rcId), null, null, rcId, null, evidence);
    }

    @Override
    public AiOutputVO incident(String alertId) {
        List<String> evidence = new ArrayList<>();
        // alertId 即告警定位，本身作为一条强证据；当前库无告警表，不再另查 rc 关联
        if (StrUtil.isNotBlank(alertId)) {
            evidence.add("alert:" + alertId);
        }
        return analyze(AiOutputTypeEnum.INCIDENT, alertId, alertId, null, null, null, evidence);
    }

    /** 服务端校验通过的证据：定位、原文片段、类型(1阶段2门禁/指标3变更4历史5任务)、是否强证据 */
    private record ValidEvidence(String locator, String snippet, int type, boolean strong) {
    }

    /**
     * 归因主流程：服务端证据校验 -> 组装提示词 -> 调模型 -> 置信度计算 -> 落库。
     */
    private AiOutputVO analyze(AiOutputTypeEnum type, String bizId, String alertId, Long appId,
                              Long rcId, Long runId, List<String> rawEvidence) {
        long start = System.currentTimeMillis();
        // 服务端逐条回查校验：阶段/任务执行结果非空、commit 非空、门禁/发布单记录存在，无效剔除
        List<ValidEvidence> effective = new ArrayList<>();
        if (CollUtil.isNotEmpty(rawEvidence)) {
            for (String locator : rawEvidence) {
                ValidEvidence validated = validate(locator);
                if (validated != null) {
                    effective.add(validated);
                }
            }
        }

        AiOutput output = new AiOutput();
        output.setType(type.getCode());
        output.setBizId(bizId);
        output.setAppId(appId);
        output.setRunId(runId);
        output.setRcId(rcId);
        output.setAlertId(alertId);
        output.setModelVersion(AiUtil.model());
        output.setPromptVersion("p-v1");

        // 无有效证据：直接落「无法归因」，不调模型
        if (effective.isEmpty()) {
            output.setStatus(AiOutputStatusEnum.NO_EVIDENCE.getCode());
            output.setConclusion("无有效证据，无法归因");
            output.setDuration(System.currentTimeMillis() - start);
            baseMapper.insert(output);
            return render(output, new ArrayList<>());
        }

        String promptEvidence = effective.stream()
                .map(e -> e.locator() + " " + e.snippet())
                .collect(Collectors.joining("\n"));
        String prompt = buildPrompt(type.getDesc(), bizId, promptEvidence);
        String answer = AiUtil.chat(prompt);
        // 模型不可用（返回 null）：落业务兜底结论，仍视为成功但置信度强制降为低
        boolean degraded = answer == null;
        String conclusion = degraded ? FALLBACK_CONCLUSION : answer;

        // 服务端按证据数量与来源数计算置信度：≥3 条不同来源为高；1 条强证据为中；其余低
        Set<String> sources = new LinkedHashSet<>();
        for (ValidEvidence e : effective) {
            sources.add(sourceName(e.type()));
        }
        int confidence;
        if (degraded) {
            confidence = AiConfidenceEnum.LOW.getCode();
        } else if (effective.size() >= IntConst.THREE && sources.size() >= IntConst.THREE) {
            confidence = AiConfidenceEnum.HIGH.getCode();
        } else if (effective.size() >= IntConst.ONE && effective.stream().anyMatch(ValidEvidence::strong)) {
            confidence = AiConfidenceEnum.MEDIUM.getCode();
        } else {
            confidence = AiConfidenceEnum.LOW.getCode();
        }

        output.setStatus(AiOutputStatusEnum.SUCCESS.getCode());
        output.setConclusion(conclusion);
        output.setCategory(extractCategory(conclusion));
        output.setConfidence(confidence);
        output.setDuration(System.currentTimeMillis() - start);
        baseMapper.insert(output);

        // 有效证据落 dev_ai_evidence
        List<AiEvidenceVO> evidenceVOs = new ArrayList<>();
        int sort = 0;
        for (ValidEvidence e : effective) {
            AiEvidence evidence = new AiEvidence();
            evidence.setOutputId(output.getId());
            evidence.setType(e.type());
            evidence.setLocator(e.locator());
            evidence.setSnippet(e.snippet());
            evidence.setIsValid(IntConst.ONE);
            evidence.setSort(sort);
            evidenceMapper.insert(evidence);
            evidenceVOs.add(BeanUtil.copy(evidence, AiEvidenceVO.class));
            sort++;
        }
        return render(output, evidenceVOs);
    }

    /**
     * 拼装归因提示词：业务侧自定用户提示词，系统提示词由 common-ai 统一配置注入。
     */
    private String buildPrompt(String type, String bizId, String evidences) {
        return "你是 DevOps 归因助手。业务类型：" + type + "，业务id：" + bizId + "。\n"
                + "请仅依据以下证据输出固定 JSON：{category,conclusion,confidence,evidence,suggestion}。\n"
                + "不得臆造证据；无可靠证据时 conclusion 输出“无有效证据，无法归因”。\n"
                + "证据列表：\n" + evidences;
    }

    /**
     * 逐条校验证据定位：按前缀解析出业务 id 并回查数据库，取不到即判定无效。
     */
    private ValidEvidence validate(String locator) {
        if (StrUtil.isBlank(locator)) {
            return null;
        }
        try {
            if (locator.startsWith("stage:")) {
                // stage:{stageId} —— 阶段必须存在且执行结果非空，摘要取 result JSON
                long stageId = Long.parseLong(locator.substring(6));
                RunStage stage = stageMapper.selectById(stageId);
                if (stage == null || StrUtil.isBlank(stage.getResult())) {
                    return null;
                }
                return new ValidEvidence(locator, stageSnippet(stage), 1, true);
            }
            if (locator.startsWith("task:")) {
                // task:{taskId} —— 任务必须存在且执行结果非空，摘要取 result JSON
                long taskId = Long.parseLong(locator.substring(5));
                RunStageTask task = taskMapper.selectById(taskId);
                if (task == null || StrUtil.isBlank(task.getResult())) {
                    return null;
                }
                return new ValidEvidence(locator, taskSnippet(task), 5, true);
            }
            if (locator.startsWith("commit:")) {
                // commit:{commitId} —— 非空即强证据
                String commit = locator.substring(7);
                if (StrUtil.isBlank(commit)) {
                    return null;
                }
                return new ValidEvidence(locator, "commit " + commit, 3, true);
            }
            if (locator.startsWith("gate:")) {
                // gate:{gateId}:{conclusion} —— 门禁记录必须存在
                String rest = locator.substring(5);
                int idx = rest.indexOf(':');
                if (idx <= 0) {
                    return null;
                }
                long gateId = Long.parseLong(rest.substring(0, idx));
                RunStageGate gate = gateResultMapper.selectById(gateId);
                if (gate == null) {
                    return null;
                }
                StringBuilder snippet = new StringBuilder("门禁").append(gateConclusionText(gate.getStatus()));
                if (StrUtil.isNotBlank(gate.getCurrentValue())) {
                    snippet.append(" 当前值=").append(gate.getCurrentValue());
                }
                if (StrUtil.isNotBlank(gate.getThreshold())) {
                    snippet.append(" 阈值=").append(gate.getThreshold());
                }
                return new ValidEvidence(locator, snippet.toString(), 2, true);
            }
            if (locator.startsWith("rc:")) {
                // rc:{rcId}:desc|risk:{content} —— 发布单记录必须存在
                String rest = locator.substring(3);
                int idx = rest.indexOf(':');
                if (idx <= 0) {
                    return null;
                }
                long rcId = Long.parseLong(rest.substring(0, idx));
                if (releaseCandidateMapper.selectById(rcId) == null) {
                    return null;
                }
                return new ValidEvidence(locator, oneLine(rest.substring(idx + 1)), 4, false);
            }
            if (locator.startsWith("alert:")) {
                // alert:{alertId} —— 告警定位本身即强证据
                String alert = locator.substring(6);
                if (StrUtil.isBlank(alert)) {
                    return null;
                }
                return new ValidEvidence(locator, "告警 " + alert, 2, true);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    /** 阶段执行结果摘要：类型 + 名称 + 状态 + 耗时 + Jenkins/门禁结论 */
    private String stageSnippet(RunStage stage) {
        String json = stage.getResult();
        StringBuilder sb = new StringBuilder("阶段[").append(stage.getName()).append("] ");
        appendResult(sb, json);
        JsonNode gate = JsonUtil.getNode(json, "gate");
        if (gate != null && !gate.isNull()) {
            String conclusion = JsonUtil.getStr(gate.toString(), "gate");
            if (StrUtil.isNotBlank(conclusion)) {
                sb.append(" 门禁结论=").append(conclusion);
            }
        }
        return oneLine(sb.toString());
    }

    /** 任务执行结果摘要：名称 + 状态 + 执行器 + 脚本 + 耗时 */
    private String taskSnippet(RunStageTask task) {
        String json = task.getResult();
        StringBuilder sb = new StringBuilder("任务[").append(task.getName()).append("] ");
        appendResult(sb, json);
        String executor = JsonUtil.getStr(json, "executor");
        if (StrUtil.isNotBlank(executor)) {
            sb.append(" 执行器=").append(executor);
        }
        String scriptRef = JsonUtil.getStr(json, "scriptRef");
        if (StrUtil.isNotBlank(scriptRef)) {
            sb.append(" 脚本=").append(scriptRef);
        }
        return oneLine(sb.toString());
    }

    /** 提取 result 共性字段：status/desc/durationMs/jenkinsStatus */
    private void appendResult(StringBuilder sb, String json) {
        String status = JsonUtil.getStr(json, "status");
        if (StrUtil.isNotBlank(status)) {
            sb.append("状态=").append(status);
        }
        String desc = JsonUtil.getStr(json, "desc");
        if (StrUtil.isNotBlank(desc) && !desc.equals(status)) {
            sb.append("(").append(desc).append(")");
        }
        Long durationMs = JsonUtil.getLong(json, "durationMs");
        if (durationMs != null) {
            sb.append(" 耗时=").append(durationMs).append("ms");
        }
        String jenkinsStatus = JsonUtil.getStr(json, "jenkinsStatus");
        if (StrUtil.isNotBlank(jenkinsStatus)) {
            sb.append(" Jenkins=").append(jenkinsStatus);
        }
    }

    /** 换行/制表符压成空格，避免污染单行证据文本 */
    private String oneLine(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    /** 门禁结论文案 */
    private String gateConclusionText(Integer conclusion) {
        if (conclusion == null) {
            return "未知";
        }
        return switch (conclusion) {
            case 1 -> "通过";
            case 2 -> "不通过";
            case 3 -> "不适用";
            case 4 -> "已豁免";
            case 5 -> "数据获取失败";
            default -> "未知";
        };
    }

    private String sourceName(int type) {
        return switch (type) {
            case 1 -> "阶段";
            case 2 -> "门禁";
            case 3 -> "变更";
            case 5 -> "任务";
            default -> "历史";
        };
    }

    /** 从模型结论里粗提取根因类别，取不到归「其他」 */
    private String extractCategory(String conclusion) {
        if (conclusion == null) {
            return "其他";
        }
        String text = conclusion.toUpperCase();
        if (text.contains("构建") || text.contains("编译") || text.contains("BUILD") || text.contains("COMPILE")) {
            return "BUILD";
        }
        if (text.contains("依赖") || text.contains("冲突") || text.contains("DEPEND")) {
            return "依赖冲突";
        }
        if (text.contains("环境") || text.contains("ENV")) {
            return "环境异常";
        }
        if (text.contains("用例") || text.contains("测试") || text.contains("FLAKY")) {
            return "用例不稳定";
        }
        if (text.contains("资源") || text.contains("内存") || text.contains("OOM")) {
            return "资源不足";
        }
        if (text.contains("配置") || text.contains("CONFIG")) {
            return "配置错误";
        }
        return "其他";
    }

    private AiOutputVO render(AiOutput output, List<AiEvidenceVO> evidences) {
        AiOutputVO vo = BeanUtil.copy(output, AiOutputVO.class);
        vo.setEvidence(evidences);
        return vo;
    }

    @Override
    public IPage<AiOutputVO> outputPage(AiOutputQuery query) {
        AiOutputWrapper wrapper = AiOutputWrapper.build();
        return wrapper.pageVo(page(Page.create(query), wrapper.lambda(query)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean feedback(AiFeedbackDTO body) {
        AiOutput output = baseMapper.selectById(body.getOutputId());
        if (output == null) {
            throw new ServiceException("AI 输出不存在");
        }
        AiFeedback feedback = new AiFeedback();
        feedback.setOutputId(body.getOutputId());
        feedback.setIsHelpful(body.getIsHelpful());
        feedback.setCorrectAnswer(body.getCorrectAnswer());
        feedback.setUserId(AuthUtil.getUserId());
        feedbackMapper.insert(feedback);
        // 低分样本进入评测集
        if (ObjUtil.equals(body.getIsHelpful(), IntConst.ZERO)) {
            AiEvalSample sample = new AiEvalSample();
            sample.setOutputId(body.getOutputId());
            sample.setSource(IntConst.ONE);
            sample.setNote(body.getCorrectAnswer());
            evalSampleMapper.insert(sample);
        }
        return true;
    }

    @Override
    public IPage<AiFeedbackVO> feedbackPage(AiFeedbackQuery query) {
        IPage<AiFeedback> pages = feedbackMapper.selectPage(Page.create(query), Wrappers.<AiFeedback>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getOutputId()), AiFeedback::getOutputId, query.getOutputId())
                .eq(ObjUtil.isNotEmpty(query.getIsHelpful()), AiFeedback::getIsHelpful, query.getIsHelpful())
                .orderByDesc(AiFeedback::getCreateTime));
        com.baomidou.mybatisplus.core.metadata.IPage<AiFeedbackVO> result =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pages.getCurrent(), pages.getSize(), pages.getTotal());
        List<AiFeedbackVO> records = new ArrayList<>();
        for (AiFeedback item : pages.getRecords()) {
            records.add(BeanUtil.copy(item, AiFeedbackVO.class));
        }
        result.setRecords(records);
        return result;
    }

    @Override
    public AppAiLevelVO appLevel(Long appId) {
        AppAiLevel level = appAiLevelMapper.selectOne(Wrappers.<AppAiLevel>lambdaQuery()
                .eq(AppAiLevel::getAppId, appId));
        if (level == null) {
            AppAiLevelVO vo = new AppAiLevelVO();
            vo.setAppId(appId);
            vo.setLevel(IntConst.ZERO);
            return vo;
        }
        return BeanUtil.copy(level, AppAiLevelVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean grantLevel(AppAiLevelDTO body) {
        AppAiLevel level = appAiLevelMapper.selectOne(Wrappers.<AppAiLevel>lambdaQuery()
                .eq(AppAiLevel::getAppId, body.getAppId()));
        if (level == null) {
            level = new AppAiLevel();
            level.setAppId(body.getAppId());
            level.setLevel(body.getLevel());
            level.setGrantedBy(AuthUtil.getUserId());
            level.setGrantedAt(new Date());
            return appAiLevelMapper.insert(level) > IntConst.ZERO;
        }
        level.setLevel(body.getLevel());
        level.setGrantedBy(AuthUtil.getUserId());
        level.setGrantedAt(new Date());
        return appAiLevelMapper.updateById(level) > IntConst.ZERO;
    }

    @Override
    public AiQualityVO quality() {
        AiQualityVO vo = new AiQualityVO();
        long total = baseMapper.countAll();
        long adopted = baseMapper.countAdopted();
        long low = baseMapper.countLowConfidence();
        long refusal = baseMapper.countRefusal();
        vo.setTotalOutput(total);
        vo.setAdoptedCount(adopted);
        vo.setLowConfidenceCount(low);
        vo.setRefusalCount(refusal);
        vo.setAdoptRate(ratio(adopted, total));
        vo.setLowConfidenceRate(ratio(low, total));
        vo.setRefusalRate(ratio(refusal, total));
        return vo;
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator == IntConst.ZERO) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }
}
