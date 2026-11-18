package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 技术债误报申请表单。
 */
@Data
public class FalsePositiveApplyDTO {

    @Schema(description = "问题id")
    @NotNull(message = "问题id不能为空")
    private Long id;

    @Schema(description = "误报理由")
    @NotBlank(message = "误报理由不能为空")
    private String reason;
}
