package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 环境列表查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnvironmentQuery extends Query {

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "环境名")
    private String name;

    @Schema(description = "环境类别")
    private Integer category;

    @Schema(description = "部署类型")
    private Integer deployType;

    @Schema(description = "状态")
    private Integer status;
}