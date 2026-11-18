package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 制品清理策略表单。
 */
@Data
public class ArtifactCleanPolicyDTO {

    @Schema(description = "策略id")
    private Long id;

    @Schema(description = "应用id，全局写 0")
    @NotNull(message = "应用id不能为空")
    private Long appId;

    @Schema(description = "保留最近 N 个版本")
    @NotNull(message = "保留数不能为空")
    private Integer keepLatest;

    @Schema(description = "永久保留生产用过的版本")
    private Integer isKeepProd;

    @Schema(description = "软删除保留天数")
    @NotNull(message = "软删除天数不能为空")
    private Integer softDeleteDays;

    @Schema(description = "执行时间")
    private String cron;

    @Schema(description = "自动执行")
    private Integer isAutoExec;
}
