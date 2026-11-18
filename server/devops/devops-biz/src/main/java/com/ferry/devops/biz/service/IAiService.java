package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.AiFeedbackDTO;
import com.ferry.devops.api.dto.AppAiLevelDTO;
import com.ferry.devops.api.dto.query.AiFeedbackQuery;
import com.ferry.devops.api.dto.query.AiOutputQuery;
import com.ferry.devops.api.entity.AiOutput;
import com.ferry.devops.api.vo.AiFeedbackVO;
import com.ferry.devops.api.vo.AiOutputVO;
import com.ferry.devops.api.vo.AiQualityVO;
import com.ferry.devops.api.vo.AppAiLevelVO;

/**
 * AI 归因、反馈与质量服务
 */
public interface IAiService extends IService<AiOutput> {

    /** 失败归因 */
    AiOutputVO attribution(Long runId);

    /** 准入辅助 */
    AiOutputVO admission(Long rcId);

    /** 发布预案 */
    AiOutputVO plan(Long rcId);

    /** 异常归因 */
    AiOutputVO incident(String alertId);

    /** AI 输出分页 */
    IPage<AiOutputVO> outputPage(AiOutputQuery query);

    /** 提交反馈(没帮助时自动进入低分评测样本) */
    boolean feedback(AiFeedbackDTO body);

    /** 反馈分页 */
    IPage<AiFeedbackVO> feedbackPage(AiFeedbackQuery query);

    /** 应用 AI 授权查询 */
    AppAiLevelVO appLevel(Long appId);

    /** 授予应用 AI 级别 */
    boolean grantLevel(AppAiLevelDTO body);

    /** AI 质量看板聚合 */
    AiQualityVO quality();
}
