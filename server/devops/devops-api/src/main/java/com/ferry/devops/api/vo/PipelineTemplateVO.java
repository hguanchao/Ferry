package com.ferry.devops.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class PipelineTemplateVO {

    @Schema(description = "模板id")
    private Long id;

    @Schema(description = "模板层级 0：组织级 1：团队级")
    private Integer scope;

    @Schema(description = "归属部门id，组织级为空")
    private Long deptId;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "适用场景")
    private String description;

    @Schema(description = "状态 0：停用 1：启用")
    private Integer status;

    @Schema(description = "当前版本id")
    private Long currentVersionId;

    @Schema(description = "当前版本号")
    private Integer currentVersionNo;

    @Schema(description = "阶段数")
    private Integer stageCount;

    @Schema(description = "占位变量数")
    private Integer variableCount;

    @Schema(description = "被派生流水线数")
    private Long derivedCount;

    @Schema(description = "创建人名称快照")
    private String createUserName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date updateTime;

    @Schema(description = "当前定义，仅详情返回")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PipelineTemplateDefinitionVO definition;
}
