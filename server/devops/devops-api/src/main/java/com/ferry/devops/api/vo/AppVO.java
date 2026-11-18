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

    @Schema(description = "所属团队")
    private Long teamId;

    @Schema(description = "负责人id，逗号分隔")
    private String ownerIds;

    @Schema(description = "负责人名称，逗号分隔")
    private String ownerNames;

    @Schema(description = "核心链路")
    private Integer isCore;

    @Schema(description = "代码平台")
    private Integer platform;

    @Schema(description = "仓库地址")
    private String repoUrl;

    @Schema(description = "默认分支")
    private String defaultBranch;

    @Schema(description = "接入完善度")
    private Integer setupStatus;

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
    private java.util.Date createTime;
}