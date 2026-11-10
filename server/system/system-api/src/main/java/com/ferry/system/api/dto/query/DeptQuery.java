package com.ferry.system.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeptQuery extends Query {

    @Schema(description = "租户编码")
    private String tenantId;

    @Schema(description = "部门名称")
    private String name;

    @Schema(description = "部门状态 0：禁用 1：正常")
    private Integer status;
}