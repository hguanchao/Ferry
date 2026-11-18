package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 流水线模板
 */
@Data
public class PipelineTemplateVO {

    @Schema(description = "模板id")
    private Long id;

    @Schema(description = "1 组织级 2 团队级")
    private Integer scope;

    @Schema(description = "团队id")
    private Long teamId;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "适用场景")
    private String scene;

    @Schema(description = "启停")
    private Integer isEnabled;

    @Schema(description = "当前版本id")
    private Long currentVersionId;

    @Schema(description = "当前版本号")
    private Integer currentVersionNo;

    @Schema(description = "阶段数")
    private Integer stageCount;

    @Schema(description = "占位变量数")
    private Integer variableCount;

    @Schema(description = "派生数")
    private Integer derivedCount;

    @Schema(description = "创建人")
    private Long createUser;

    @Schema(description = "创建时间")
    private Date createTime;
}
