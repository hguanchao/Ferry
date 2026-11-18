package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布单制品项
 */
@Data
public class ReleaseArtifactVO {

    @Schema(description = "记录id")
    private Long id;

    @Schema(description = "发布单id")
    private Long rcId;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "制品id")
    private Long artifactId;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "摘要")
    private String digest;

    @Schema(description = "门禁汇总")
    private Integer gateConclusion;

    @Schema(description = "是否锁定")
    private Integer isLocked;
}
