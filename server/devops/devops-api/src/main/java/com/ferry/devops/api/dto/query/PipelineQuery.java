package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 流水线列表查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PipelineQuery extends Query {

    @Schema(description = "流水线名称")
    private String name;

    @Schema(description = "状态 0停用 1启用")
    private Integer status;

    @Schema(description = "应用id")
    private List<Long> appIds;
}
