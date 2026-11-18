package com.ferry.devops.api.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 应用详情
 */
@Data
public class AppDetailVO {

    @Schema(description = "应用id")
    private Long id;

    @Schema(description = "应用编码")
    private String code;

    @Schema(description = "应用名称")
    private String name;

    @Schema(description = "所属团队")
    private Long teamId;

    @Schema(description = "负责人id")
    private String ownerIds;

    @Schema(description = "负责人名称")
    private String ownerNames;

    @Schema(description = "核心链路")
    private Integer isCore;

    @Schema(description = "代码平台")
    private Integer platform;

    @Schema(description = "默认分支")
    private String defaultBranch;

    @Schema(description = "默认提交")
    private String defaultCommitId;

    @Schema(description = "对象存储 bucket")
    private String storageBucket;

    @Schema(description = "对象存储路径")
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

    @Schema(description = "接入完善度")
    private Integer setupStatus;

    @Schema(description = "代码源列表")
    private java.util.List<AppRepoVO> repos;

    @Schema(description = "负责人列表")
    private java.util.List<AppOwnerVO> owners;

    @Schema(description = "依赖服务列表")
    private java.util.List<AppDependencyVO> dependencies;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private java.util.Date createTime;
}