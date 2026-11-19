package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 接入向导进度
 */
@Data
public class WizardProgressVO {

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "已完成步骤")
    private java.util.List<Integer> completedSteps;

    @Schema(description = "已跳过步骤")
    private java.util.List<Integer> skippedSteps;

    @Schema(description = "应用状态 0停用 1启用 2草稿 3完善")
    private Integer status;
}