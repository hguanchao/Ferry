package com.ferry.devops.api.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 应用列表项
 */
@Data
public class AppVO {

    @Schema(description = "应用id")
    private Long id;

    @Schema(description = "应用编码")
    private String code;

    @Schema(description = "应用名称")
    private String name;

    @Schema(description = "所属团队id")
    private Long deptId;

    @Schema(description = "所属团队名称，联表 sys_dept 获取")
    private String deptName;

    @Schema(description = "应用负责人id")
    private Long ownerId;

    @Schema(description = "应用负责人名称，联表 sys_user 获取")
    private String ownerName;


    @Schema(description = "代码平台 1GitLab 2GitHub 3Gitee")
    private Integer repoType;

    @Schema(description = "仓库git地址")
    private String repoUrl;

    @Schema(description = "仓库分支")
    private String repoBranch;

    @Schema(description = "仓库提交id")
    private String repoCommitId;

    @Schema(description = "应用描述")
    private String description;

    @Schema(description = "应用状态 0停用 1启用 2草稿 3完善")
    private Integer status;

    @Schema(description = "生产部署类型")
    private String prodDeployTypes;

    @Schema(description = "流水线数")
    private Integer pipelineCount;

    @Schema(description = "最近运行状态")
    private Integer lastRunStatus;

    @Schema(description = "最近运行时间")
    private String lastRunTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
