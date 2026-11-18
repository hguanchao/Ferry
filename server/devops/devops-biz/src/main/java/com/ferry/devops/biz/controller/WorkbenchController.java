package com.ferry.devops.biz.controller;

import com.ferry.common.core.http.R;
import com.ferry.devops.biz.service.IMetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 工作台控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/workbench")
@Tag(name = "工作台", description = "平台聚合概览")
public class WorkbenchController {
    private final IMetricService metricService;

    @GetMapping("/overview")
    @Operation(summary = "工作台聚合概览(登录即可)")
    public R<Map<String, Object>> overview() {
        return R.data(metricService.overview());
    }
}
