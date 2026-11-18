package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 制品入库表单。入库为系统自动动作，本 DTO 供回传复核入口使用。
 */
@Data
public class ArtifactDTO {

    @Schema(description = "应用id")
    @NotNull(message = "应用id不能为空")
    private Long appId;

    @Schema(description = "版本号")
    @NotBlank(message = "版本号不能为空")
    private String version;

    @Schema(description = "制品形态 1 包制品 2 镜像制品")
    @NotNull(message = "制品形态不能为空")
    private Integer form;

    @Schema(description = "sha256 摘要")
    @NotBlank(message = "摘要不能为空")
    private String digest;

    @Schema(description = "字节数")
    @NotNull(message = "大小不能为空")
    private Long size;

    @Schema(description = "产出运行")
    @NotNull(message = "运行id不能为空")
    private Long runId;

    @Schema(description = "Jenkins 构建号")
    private Long buildNo;

    @Schema(description = "构建镜像")
    private String buildImage;

    @Schema(description = "分支")
    private String branch;

    @Schema(description = "提交")
    private String commitId;

    @Schema(description = "构建人")
    private Long builder;

    @Schema(description = "门禁汇总结论")
    private Integer gateConclusion;
}
