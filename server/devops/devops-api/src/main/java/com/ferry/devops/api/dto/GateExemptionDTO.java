package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 门禁豁免申请表单。
 */
@Data
public class GateExemptionDTO {

    @Schema(description = "门禁结果id")
    @NotNull(message = "结果id不能为空")
    private Long resultId;

    @Schema(description = "理由，不少于 20 字")
    @NotBlank(message = "豁免理由不能为空")
    private String reason;

    @Schema(description = "有效期小时 8/24/72")
    @NotNull(message = "有效期不能为空")
    @Min(value = 1, message = "有效期至少 1 小时")
    @Max(value = 72, message = "有效期不得超过 72 小时")
    private Integer hours;

    @Schema(description = "关联问题单")
    private String issueNo;
}
