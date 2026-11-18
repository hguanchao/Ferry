package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 流水线版本快照摘要
 */
@Data
public class PipelineVersionVO {

    @Schema(description = "版本id")
    private Long id;

    @Schema(description = "流水线id")
    private Long pipelineId;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "内容摘要")
    private String contentHash;

    @Schema(description = "变更说明")
    private String changeNote;

    @Schema(description = "回退来源版本")
    private Long sourceVersionId;

    @Schema(description = "1表单 2YAML 3回退 4模板合并")
    private Integer sourceType;

    @Schema(description = "创建人")
    private Long createUser;

    @Schema(description = "创建时间")
    private Date createTime;
}
