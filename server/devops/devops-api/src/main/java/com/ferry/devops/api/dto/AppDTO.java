package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @Schema(description = "所属团队")
    @NotNull(message = "所属团队不能为空")
    private Long teamId;

    @Schema(description = "负责人用户id，逗号分隔，1-3 人")
    @NotBlank(message = "应用负责人不能为空")
    private String ownerIds;

    @Schema(description = "备份负责人用户id，逗号分隔")
    private String backupOwnerIds;

    @Schema(description = "核心链路 0 否 1 是")
    private Integer isCore;

    @Schema(description = "代码平台 1 GitLab 2 GitHub 3 Gitee")
    @NotNull(message = "代码平台不能为空")
    private Integer platform;

    @Schema(description = "仓库路径")
    private String repoPath;

    @Schema(description = "默认分支")
    private String defaultBranch;

    @Schema(description = "指定提交id")
    private String defaultCommitId;

    @Schema(description = "对象存储 bucket")
    private String storageBucket;

    @Schema(description = "对象存储路径前缀")
    private String storagePath;

    @Schema(description = "OCI Registry 仓库名")
    private String registryRepo;

    @Schema(description = "监控系统类型")
    private String monitorType;

    @Schema(description = "监控大盘地址")
    private String monitorDashboard;

    @Schema(description = "告警规则组")
    private String alertGroup;

    @Schema(description = "值班时段")
    private String oncallWindow;

    @Schema(description = "描述")
    private String description;
}