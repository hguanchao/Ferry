package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppDTO {

    @Schema(description = "应用id")
    private Long id;

    @Schema(description = "应用编码，创建后不可改")
    @NotBlank(message = "应用编码不能为空")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,39}$", message = "应用编码只能是小写字母或数字开头，可含中划线，长度 2-40")
    private String code;

    @Schema(description = "应用名称")
    @NotBlank(message = "应用名称不能为空")
    @Size(max = 100, message = "应用名称不能超过 100 个字符")
    private String name;

    @Schema(description = "所属团队（部门id）")
    @NotNull(message = "请选择所属团队")
    private Long deptId;

    @Schema(description = "应用负责人id，逗号分隔，1-3 人")
    @NotBlank(message = "请选择应用负责人")
    @Size(max = 200, message = "应用负责人id不能超过 200 个字符")
    private String ownerIds;

    @Schema(description = "应用负责人名称，逗号分隔，与 ownerIds 顺序一致")
    @Size(max = 500, message = "应用负责人名称不能超过 500 个字符")
    private String ownerNames;

    @Schema(description = "应用描述")
    @Size(max = 500, message = "应用描述不能超过 500 个字符")
    private String description;

    @Schema(description = "核心链路 0：否 1：是")
    private Integer isCore;

    @Schema(description = "状态 0：停用 1：正常")
    private Integer status;

    @Schema(description = "代码托管平台 GITLAB/GITHUB/GITEE")
    private String repoType;

    @Schema(description = "仓库 Git 地址")
    @Size(max = 500, message = "仓库地址不能超过 500 个字符")
    private String repoUrl;

    @Schema(description = "默认分支")
    @Size(max = 100, message = "默认分支不能超过 100 个字符")
    private String repoBranch;

    @Schema(description = "指定提交id，留空取默认分支最新提交")
    @Pattern(regexp = "^$|^[0-9a-fA-F]{7,40}$", message = "提交id 只能是 7-40 位十六进制")
    private String repoCommitId;

    @Schema(description = "平台凭据id")
    private Long repoCredentialId;
}
