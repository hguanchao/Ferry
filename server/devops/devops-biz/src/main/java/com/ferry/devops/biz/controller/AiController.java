package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.AiFeedbackDTO;
import com.ferry.devops.api.dto.AppAiLevelDTO;
import com.ferry.devops.api.dto.query.AiFeedbackQuery;
import com.ferry.devops.api.dto.query.AiOutputQuery;
import com.ferry.devops.api.vo.AiFeedbackVO;
import com.ferry.devops.api.vo.AiOutputVO;
import com.ferry.devops.api.vo.AiQualityVO;
import com.ferry.devops.api.vo.AppAiLevelVO;
import com.ferry.devops.biz.service.IAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 归因、反馈与质量控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/ai")
@Tag(name = "AI 助手", description = "AI 归因、反馈与质量")
public class AiController {
    private final IAiService service;

    @PostMapping("/attribution")
    @PreAuthorize("devops:ai:attribution")
    @Operation(summary = "AI 失败归因")
    public R<AiOutputVO> attribution(@RequestBody java.util.Map<String, Object> body) {
        return R.data(service.attribution(Long.valueOf(String.valueOf(body.get("runId")))));
    }

    @PostMapping("/admission")
    @PreAuthorize("devops:ai:admission")
    @Operation(summary = "AI 准入辅助")
    public R<AiOutputVO> admission(@RequestBody java.util.Map<String, Object> body) {
        return R.data(service.admission(Long.valueOf(String.valueOf(body.get("rcId")))));
    }

    @PostMapping("/plan")
    @PreAuthorize("devops:ai:admission")
    @Operation(summary = "AI 发布预案")
    public R<AiOutputVO> plan(@RequestBody java.util.Map<String, Object> body) {
        return R.data(service.plan(Long.valueOf(String.valueOf(body.get("rcId")))));
    }

    @PostMapping("/incident")
    @PreAuthorize("devops:ai:attribution")
    @Operation(summary = "AI 生产异常归因")
    public R<AiOutputVO> incident(@RequestBody java.util.Map<String, Object> body) {
        return R.data(service.incident(String.valueOf(body.get("alertId"))));
    }

    @PostMapping("/output/pageList")
    @PreAuthorize("devops:ai:attribution")
    @Operation(summary = "AI 输出分页")
    public R<IPage<AiOutputVO>> outputPage(@RequestBody AiOutputQuery body) {
        return R.data(service.outputPage(body));
    }

    @PostMapping("/feedback")
    @PreAuthorize("devops:ai:feedback")
    @Operation(summary = "提交 AI 反馈")
    public R<String> feedback(@RequestBody @Valid AiFeedbackDTO body) {
        return service.feedback(body) ? R.success("反馈成功") : R.fail("反馈失败");
    }

    @PostMapping("/feedback/pageList")
    @PreAuthorize("devops:ai:feedback")
    @Operation(summary = "AI 反馈分页")
    public R<IPage<AiFeedbackVO>> feedbackPage(@RequestBody AiFeedbackQuery body) {
        return R.data(service.feedbackPage(body));
    }

    @GetMapping("/appLevel")
    @PreAuthorize("devops:action:grant")
    @Operation(summary = "应用 AI 授权查询")
    public R<AppAiLevelVO> appLevel(@RequestParam("appId") Long appId) {
        return R.data(service.appLevel(appId));
    }

    @PostMapping("/appLevel/grant")
    @PreAuthorize("devops:action:grant")
    @Operation(summary = "授予应用 AI 级别")
    public R<String> grantLevel(@RequestBody @Valid AppAiLevelDTO body) {
        return service.grantLevel(body) ? R.success("授权成功") : R.fail("授权失败");
    }

    @GetMapping("/quality")
    @PreAuthorize("devops:ai:quality")
    @Operation(summary = "AI 质量看板")
    public R<AiQualityVO> quality() {
        return R.data(service.quality());
    }
}
