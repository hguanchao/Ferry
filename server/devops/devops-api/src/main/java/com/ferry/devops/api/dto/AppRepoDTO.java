package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 应用代码源表单
 */
@Data
public class AppRepoDTO {

    @Schema(description = "数据id")
    private Long id;

    @Schema(description = "应用id")
    @NotNull(message = "应用id不能为空")
    private Long appId;

    @Schema(description = "平台枚举")
    @NotNull(message = "代码平台不能为空")
    private Integer platform;

    @Schema(description = "group/project 路径")
    @NotBlank(message = "仓库路径不能为空")
    private String repoPath;

    @Schema(description = "完整 Git 地址")
    @NotBlank(message = "仓库地址不能为空")
    private String repoUrl;

    @Schema(description = "凭据id")
    private Long credentialId;

    @Schema(description = "是否主仓库")
    private Integer isMain;

    @Schema(description = "该仓库默认分支")
    private String defaultBranch;

    @Schema(description = "是否公开仓库")
    private Integer isPublic;
}