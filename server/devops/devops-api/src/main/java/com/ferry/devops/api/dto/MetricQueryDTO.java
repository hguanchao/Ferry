package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 度量查询表单
 */
@Data
public class MetricQueryDTO {

    @Schema(description = "应用id列表")
    private List<Long> appIds;

    @Schema(description = "开始日期 yyyy-MM-dd")
    private String start;

    @Schema(description = "结束日期 yyyy-MM-dd")
    private String end;
}
