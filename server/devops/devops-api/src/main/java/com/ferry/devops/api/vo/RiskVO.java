package com.ferry.devops.api.vo;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 风险评级结果
 */
@Data
public class RiskVO {

    @Schema(description = "风险等级 1低 2中 3高")
    private Integer level;

    @Schema(description = "加权分")
    private java.math.BigDecimal score;

    @Schema(description = "五因子明细")
    private String detail;
}
