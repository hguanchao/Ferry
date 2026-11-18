package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 依赖服务
 */
@Data
public class AppDependencyVO {

    @Schema(description = "数据id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "依赖服务名")
    private String depName;

    @Schema(description = "关联应用id")
    private Long depAppId;

    @Schema(description = "版本范围")
    private String versionRange;
}