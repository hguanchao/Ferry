package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 运行列表查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RunQuery extends Query {

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "流水线id")
    private Long pipelineId;

    @Schema(description = "运行状态")
    private List<Integer> statuses;

    @Schema(description = "分支")
    private String branch;

    @Schema(description = "开始时间起")
    private String startFrom;

    @Schema(description = "开始时间止")
    private String startTo;
}
