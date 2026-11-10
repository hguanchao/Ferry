package com.ferry.system.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MenuQuery extends Query {

    @Schema(description = "租户编码")
    private String tenantId;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "菜单类型 0：目录 1：菜单 2：按钮")
    private Integer type;

    @Schema(description = "菜单状态 0：停用 1：正常")
    private Integer status;
}
