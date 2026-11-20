package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "分支；为空时回退应用登记的默认分支")
    @Size(max = 100, message = "分支长度不能超过 100")
    private String branch;

    @Schema(description = "提交 ID / Tag / Ref；为空时回退应用登记的默认提交")
    @Size(max = 100, message = "提交或引用值长度不能超过 100")
    private String commitId;

    @Schema(description = "1 Commit 2 Tag/Ref，默认 1")
    @Min(value = 1, message = "引用类型不合法")
    @Max(value = 2, message = "引用类型不合法")
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
