package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 制品清理策略。
 */
@Data
public class CleanPolicyVO {

    @Schema(description = "策略id")
    private Long id;

    @Schema(description = "应用id，全局为 0")
    private Long appId;

    @Schema(description = "保留最近 N 个版本")
    private Integer keepLatest;

    @Schema(description = "永久保留生产用过的版本")
    private Integer isKeepProd;

    @Schema(description = "软删除保留天数")
    private Integer softDeleteDays;

    @Schema(description = "执行时间")
    private String cron;

    @Schema(description = "自动执行")
    private Integer isAutoExec;
}
