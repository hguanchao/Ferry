package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布策略项
 */
@Data
public class ReleaseStrategyVO {

    @Schema(description = "策略id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "多段发布窗口")
    private String windowsJson;

    @Schema(description = "是否排除封网")
    private Integer isClosureExcluded;

    @Schema(description = "默认观察期")
    private Integer observeMinutes;
}
