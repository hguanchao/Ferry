package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 应用代码源
 */
@Data
public class AppRepoVO {

    @Schema(description = "数据id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "平台枚举")
    private Integer platform;

    @Schema(description = "仓库路径")
    private String repoPath;

    @Schema(description = "仓库地址")
    private String repoUrl;

    @Schema(description = "凭据id")
    private Long credentialId;

    @Schema(description = "是否主仓库")
    private Integer isMain;

    @Schema(description = "默认分支")
    private String defaultBranch;

    @Schema(description = "是否公开仓库")
    private Integer isPublic;
}