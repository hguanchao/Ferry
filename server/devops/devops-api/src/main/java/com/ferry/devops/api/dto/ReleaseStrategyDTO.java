package com.ferry.devops.api.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布策略表单
 */
@Data

public class ReleaseStrategyDTO {

    @Schema(description = "应用id")
    @NotNull(message = "参数不能为空")
    private Long appId;

    @Schema(description = "多段发布窗口JSON")
    @NotNull(message = "参数不能为空")
    private String windowsJson;

    @Schema(description = "是否排除封网")
    private Integer isClosureExcluded;

    @Schema(description = "默认观察期覆盖值")
    private Integer observeMinutes;
}
