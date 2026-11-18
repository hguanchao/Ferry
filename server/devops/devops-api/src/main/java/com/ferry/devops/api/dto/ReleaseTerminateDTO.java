package com.ferry.devops.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 终止发布表单
 */
@Data

public class ReleaseTerminateDTO {

    @Schema(description = "发布单id")
    @NotNull(message = "参数不能为空")
    private Long id;

    @Schema(description = "终止原因")
    @NotBlank(message = "参数不能为空")
    private String reason;
}
