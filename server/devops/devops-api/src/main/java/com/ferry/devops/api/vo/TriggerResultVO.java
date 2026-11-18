package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 触发结果
 */
@Data
public class TriggerResultVO {

    @Schema(description = "created/merged/reused/rejected")
    private String result;

    @Schema(description = "运行id")
    private Long runId;

    @Schema(description = "复用制品摘要")
    private String artifactDigest;

    @Schema(description = "说明")
    private String message;
}
