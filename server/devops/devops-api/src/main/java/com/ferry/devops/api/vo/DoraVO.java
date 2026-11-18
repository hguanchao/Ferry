package com.ferry.devops.api.vo;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * DORA 指标 VO
 */
@Data
public class DoraVO {

    @Schema(description = "部署频率")
    private BigDecimal deploymentFrequency;

    @Schema(description = "变更前置时间秒")
    private BigDecimal leadTimeP50;

    @Schema(description = "变更失败率")
    private BigDecimal changeFailRate;

    @Schema(description = "平均恢复时长秒")
    private BigDecimal mttrP50;

    @Schema(description = "数据状态 1 正常 2 数据不足")
    private Integer dataStatus;

    @Schema(description = "数据截至日期")
    private String dataThrough;
}
