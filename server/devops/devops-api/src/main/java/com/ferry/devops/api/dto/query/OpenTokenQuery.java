package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开放 API Token 查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OpenTokenQuery extends Query {

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "状态")
    private Integer status;
}
