package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 手动触发 / 断点重跑 / 运行干预 请求
 */
@Data
public class RunTriggerDTO {

    @Schema(description = "运行id（干预/重跑时必填）")
    private Long id;

    @Schema(description = "应用id")
    @NotNull(message = "应用id不能为空")
    private Long appId;

    @Schema(description = "流水线id")
    @NotNull(message = "流水线id不能为空")
    private Long pipelineId;

    @Schema(description = "分支")
    @NotBlank(message = "分支不能为空")
    private String branch;

    @Schema(description = "提交id")
    @NotBlank(message = "提交id不能为空")
    private String commitId;

    @Schema(description = "1 Commit 2 Tag/Ref")
    private Integer refType;

    @Schema(description = "触发说明 / 干预原因")
    private String note;

    @Schema(description = "幂等请求id")
    private String requestId;

    @Schema(description = "重跑起始阶段id")
    private Long fromStageId;

    @Schema(description = "干预原因（中止必填）")
    private String reason;
}
