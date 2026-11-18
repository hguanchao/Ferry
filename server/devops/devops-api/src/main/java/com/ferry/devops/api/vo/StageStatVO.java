package com.ferry.devops.api.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 阶段耗时统计 VO
 */
@Data
public class StageStatVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "统计日")
    private Date statDate;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "阶段类型")
    private Integer stageType;

    @Schema(description = "中位耗时")
    private Long p50;

    @Schema(description = "P90 耗时")
    private Long p90;

    @Schema(description = "失败数")
    private Integer failCount;
}
