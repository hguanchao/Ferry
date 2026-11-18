package com.ferry.devops.biz.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.http.R;
import com.ferry.common.security.annotation.PreAuthorize;
import com.ferry.devops.api.dto.MetricQueryDTO;
import com.ferry.devops.api.dto.query.MetricQuery;
import com.ferry.devops.api.vo.AiQualityVO;
import com.ferry.devops.api.vo.DoraVO;
import com.ferry.devops.api.vo.MetricVO;
import com.ferry.devops.api.vo.StageStatVO;
import com.ferry.devops.biz.service.IMetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 度量控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/metric")
@Tag(name = "交付度量", description = "DORA、流水线效率、阶段耗时与 AI 效能")
public class MetricController {
    private final IMetricService service;

    @PostMapping("/dora")
    @PreAuthorize("devops:metric:view")
    @Operation(summary = "DORA 四项指标")
    public R<IPage<MetricVO>> dora(@RequestBody MetricQuery body) {
        return R.data(service.pipelineEfficiency(body));
    }

    @PostMapping("/pipelineEfficiency")
    @PreAuthorize("devops:metric:view")
    @Operation(summary = "流水线效率")
    public R<IPage<MetricVO>> pipelineEfficiency(@RequestBody MetricQuery body) {
        return R.data(service.pipelineEfficiency(body));
    }

    @PostMapping("/stageDuration")
    @PreAuthorize("devops:metric:view")
    @Operation(summary = "阶段耗时统计")
    public R<IPage<StageStatVO>> stageDuration(@RequestBody MetricQuery body) {
        return R.data(service.stageDuration(body));
    }

    @PostMapping("/aiQuality")
    @PreAuthorize("devops:metric:view")
    @Operation(summary = "AI 效能")
    public R<AiQualityVO> aiQuality(@RequestBody MetricQueryDTO body) {
        return R.data(service.aiQuality(body));
    }

    @PostMapping("/gateHealth")
    @PreAuthorize("devops:metric:view")
    @Operation(summary = "门禁健康度")
    public R<DoraVO> gateHealth(@RequestBody MetricQueryDTO body) {
        return R.data(service.gateHealth(body));
    }
}
