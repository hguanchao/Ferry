package com.ferry.devops.api.vo;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流水线列表项与详情
 */
@Data
public class PipelineVO {

    @Schema(description = "流水线id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "当前分支")
    private String repoBranch;

    @Schema(description = "流水线名称")
    private String name;

    @Schema(description = "状态 0停用 1启用")
    private Integer status;

    @Schema(description = "当前版本号")
    private Integer version;

    @Schema(description = "当前版本阶段数")
    private Integer stageCount;

    @Schema(description = "当前版本阶段定义，详情接口返回")
    private List<PipelineStageVO> stages;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date updateTime;
}
