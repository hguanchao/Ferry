package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 提示版本表单
 */
@Data
public class AiPromptVersionDTO {

    @Schema(description = "主键(编辑时传)")
    private Long id;

    @Schema(description = "版本号")
    @NotBlank(message = "版本号不能为空")
    private String code;

    @Schema(description = "提示内容")
    @NotBlank(message = "提示内容不能为空")
    private String content;

    @Schema(description = "1 草稿 2 生效 3 归档")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
