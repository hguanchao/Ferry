package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 应用概览
 */
@Data
public class AppOverviewVO {

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "流水线数")
    private Integer pipelineCount;

    @Schema(description = "环境数")
    private Integer envCount;

    @Schema(description = "运行中数量")
    private Integer runningCount;

    @Schema(description = "生产生效制品摘要")
    private String prodArtifactDigest;

    @Schema(description = "生产部署类型")
    private String prodDeployType;

    @Schema(description = "阻塞项")
    private java.util.List<java.util.Map<String,Object>> blockers;
}