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

    @Schema(description = "关键字")
    private String keyword;
}
