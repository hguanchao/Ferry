package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
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
import com.ferry.devops.api.enums.AiConfidenceEnum;
import com.ferry.devops.api.enums.AiOutputStatusEnum;
import com.ferry.devops.api.enums.AiOutputTypeEnum;
import com.ferry.devops.api.vo.AiEvidenceVO;
import com.ferry.devops.api.vo.AiFeedbackVO;
import com.ferry.devops.api.vo.AiOutputVO;
import com.ferry.devops.api.vo.AiQualityVO;
import com.ferry.devops.api.vo.AppAiLevelVO;
import com.ferry.devops.biz.integration.ai.AiClient;
import com.ferry.devops.biz.integration.ai.PromptBuilder;
import com.ferry.devops.biz.mapper.AiEvalSampleMapper;
import com.ferry.devops.biz.mapper.AiEvidenceMapper;
import com.ferry.devops.biz.mapper.AiFeedbackMapper;
import com.ferry.devops.biz.mapper.AiOutputMapper;
import com.ferry.devops.biz.mapper.AppAiLevelMapper;
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
import java.util.Set;

/**
 * AI 归因、反馈与质量服务实现
 */
@Service
@RequiredArgsConstructor
public class AiServiceImpl extends ServiceImpl<AiOutputMapper, AiOutput> implements IAiService {
    private final AiEvidenceMapper evidenceMapper;
    private final AiFeedbackMapper feedbackMapper;
    private final AiEvalSampleMapper evalSampleMapper;
    private final AppAiLevelMapper appAiLevelMapper;
    private final AiClient aiClient;
    private final PromptBuilder promptBuilder;

    @Override
    public AiOutputVO attribution(Long runId) {
        return analyze(AiOutputTypeEnum.FAILURE_ATTRIBUTION, String.valueOf(runId), null, null, null, runId, null);
    }

    @Override
    public AiOutputVO admission(Long rcId) {
        return analyze(AiOutputTypeEnum.ADMISSION, String.valueOf(rcId), null, null, rcId, null, null);
    }

    @Override
    public AiOutputVO plan(Long rcId) {
        return analyze(AiOutputTypeEnum.RELEASE_PLAN, String.valueOf(rcId), null, null, rcId, null, null);
    }

    @Override
    public AiOutputVO incident(String alertId) {
        return analyze(AiOutputTypeEnum.INCIDENT, alertId, alertId, null, null, null, null);
    }

    /**
     * 归因主流程：组装提示词 -> 调模型 -> 证据校验 -> 置信度计算 -> 落库。
     */
    private AiOutputVO analyze(AiOutputTypeEnum type, String bizId, String alertId, Long appId,
                              Long rcId, Long runId, List<String> rawEvidence) {
        long start = System.currentTimeMillis();
        // 证据定位校验(locator 非空)，无效剔除
        List<String> effective = new ArrayList<>();
        if (CollUtil.isNotEmpty(rawEvidence)) {
            for (String locator : rawEvidence) {
                if (StrUtil.isNotBlank(locator)) {
                    effective.add(locator);
                }
            }
        }
        // 无有效证据：直接落「无法归因」，不调模型
        AiOutput output = new AiOutput();
        output.setType(type.getCode());
        output.setBizId(bizId);
        output.setAppId(appId);
        output.setRunId(runId);
        output.setRcId(rcId);
        output.setAlertId(alertId);
        output.setModelVersion("min-v1");
        output.setPromptVersion("p-v1");

        if (CollUtil.isEmpty(effective)) {
            output.setStatus(AiOutputStatusEnum.NO_EVIDENCE.getCode());
            output.setConclusion("无有效证据，无法归因");
            output.setDuration(System.currentTimeMillis() - start);
            baseMapper.insert(output);
            return render(output, new ArrayList<>());
        }

        String prompt = promptBuilder.build(type.getDesc(), bizId, String.join("\n", effective));
        String conclusion = aiClient.chat(prompt);

        // 服务端按证据数量与来源数计算置信度
        Set<String> sources = new LinkedHashSet<>();
        for (String locator : effective) {
            sources.add(sourceOf(locator));
        }
        int confidence;
        if (effective.size() >= IntConst.THREE && sources.size() >= IntConst.THREE) {
            confidence = AiConfidenceEnum.HIGH.getCode();
        } else if (effective.size() >= IntConst.ONE && hasStrongEvidence(effective)) {
            confidence = AiConfidenceEnum.MEDIUM.getCode();
        } else {
            confidence = AiConfidenceEnum.LOW.getCode();
        }

        output.setStatus(AiOutputStatusEnum.SUCCESS.getCode());
        output.setConclusion(conclusion);
        output.setCategory("其他");
        output.setConfidence(confidence);
        output.setDuration(System.currentTimeMillis() - start);
        baseMapper.insert(output);

        // 落有效证据
        List<AiEvidenceVO> evidenceVOs = new ArrayList<>();
        int sort = 0;
        for (String locator : effective) {
            AiEvidence evidence = new AiEvidence();
            evidence.setOutputId(output.getId());
            evidence.setType(sourceType(sourceOf(locator)));
            evidence.setLocator(locator);
            evidence.setIsValid(IntConst.ONE);
            evidence.setSort(sort);
            evidenceMapper.insert(evidence);
            evidenceVOs.add(BeanUtil.copy(evidence, AiEvidenceVO.class));
            sort++;
        }
        return render(output, evidenceVOs);
    }

    private String sourceOf(String locator) {
        if (locator.startsWith("log:") || locator.contains(".log")) {
            return "日志";
        }
        if (locator.startsWith("change:") || locator.contains(".java") || locator.contains(".ts")) {
            return "变更";
        }
        if (locator.startsWith("history:") || locator.contains("历史")) {
            return "历史";
        }
        return "指标";
    }

    private int sourceType(String source) {
        return switch (source) {
            case "日志" -> IntConst.ONE;
            case "指标" -> 2;
            case "变更" -> 3;
            default -> 4;
        };
    }

    private boolean hasStrongEvidence(List<String> evidences) {
        return evidences.stream().anyMatch(e -> StrUtil.isNotBlank(e) && e.length() >= 20);
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
