package com.ferry.devops.api.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 准入检查项人工结论表单
 */
@Data

public class ReleaseCheckDTO {

    @Schema(description = "检查项id")
    @NotNull(message = "参数不能为空")
    private Long itemId;

    @Schema(description = "人工结论 1通过 2不通过 3不适用 4已知悉")
    @NotNull(message = "参数不能为空")
    private Integer conclusion;

    @Schema(description = "备注/理由")
    private String remark;
}
