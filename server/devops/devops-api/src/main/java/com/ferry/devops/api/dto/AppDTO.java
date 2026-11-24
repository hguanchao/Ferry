package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 应用建档/编辑表单
 */
@Data
public class AppDTO {

    @Schema(description = "应用id")
    private Long id;

    @Schema(description = "应用编码，小写字母/数字/中划线，创建后不可改")
    @NotBlank(message = "应用编码不能为空")
    private String code;

    @Schema(description = "应用名称")
    @NotBlank(message = "应用名称不能为空")
    private String name;

    @Schema(description = "所属团队id")
    @NotNull(message = "所属团队不能为空")
    private Long deptId;

    @Schema(description = "应用负责人用户id")
    @NotNull(message = "应用负责人不能为空")
    private Long ownerId;


    @Schema(description = "代码平台 1GitLab 2GitHub 3Gitee")
    @NotNull(message = "代码平台不能为空")
    private Integer repoType;

    @Schema(description = "仓库git地址")
    @NotBlank(message = "仓库地址不能为空")
    private String repoUrl;

    @Schema(description = "仓库访问令牌，公开仓库可留空")
    private String repoToken;

    @Schema(description = "仓库分支")
    private String repoBranch;

    @Schema(description = "仓库提交id，7–40 位十六进制")
    @Pattern(regexp = "^$|^[0-9a-fA-F]{7,40}$", message = "指定提交需为 7–40 位十六进制")
    private String repoCommitId;

    @Schema(description = "应用描述")
    private String description;
}
