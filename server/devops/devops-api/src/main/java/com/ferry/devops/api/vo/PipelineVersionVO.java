package com.ferry.devops.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class PipelineVersionVO {

    @Schema(description = "版本id")
    private Long id;

    @Schema(description = "流水线id")
    private Long pipelineId;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "变更说明")
    private String changeNote;

    @Schema(description = "变更人")
    private String createUserName;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;

    @Schema(description = "版本定义")
    private PipelineDefinitionVO definition;
}
