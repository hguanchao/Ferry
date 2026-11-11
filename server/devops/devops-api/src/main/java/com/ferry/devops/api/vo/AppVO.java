package com.ferry.devops.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class AppVO {

    @Schema(description = "应用id")
    private Long id;

    @Schema(description = "应用编码")
    private String code;

    @Schema(description = "应用名称")
    private String name;

    @Schema(description = "所属团队（部门id）")
    private Long deptId;

    @Schema(description = "应用负责人id，逗号分隔")
    private String ownerIds;

    @Schema(description = "应用负责人名称，逗号分隔，与 ownerIds 顺序一致")
    private String ownerNames;

    @Schema(description = "应用描述")
    private String description;

    @Schema(description = "核心链路 0：否 1：是")
    private Integer isCore;

    @Schema(description = "状态 0：停用 1：正常")
    private Integer status;

    @Schema(description = "代码托管平台 GITLAB/GITHUB/GITEE")
    private String repoType;

    @Schema(description = "仓库 Git 地址")
    private String repoUrl;

    @Schema(description = "默认分支")
    private String repoBranch;

    @Schema(description = "指定提交id")
    private String repoCommitId;

    @Schema(description = "平台凭据id")
    private Long repoCredentialId;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
