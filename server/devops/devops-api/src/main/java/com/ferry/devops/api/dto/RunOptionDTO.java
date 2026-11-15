package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 触发候选项查询
 */
@Data
public class RunOptionDTO {

    @Schema(description = "应用id")
    @NotNull(message = "请选择应用")
    private Long appId;

    @Schema(description = "分支，查询提交候选时必填")
    @Size(max = 100, message = "分支不能超过 100 个字符")
    private String branch;
}
