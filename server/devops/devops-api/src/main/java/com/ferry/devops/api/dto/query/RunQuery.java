package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流水线运行查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RunQuery extends Query {

    @Schema(description = "流水线id")
    private Long pipelineId;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "运行状态 QUEUED/RUNNING/BLOCKED/SUCCESS/FAILED/TIMEOUT/CANCELED")
    private String status;
}
