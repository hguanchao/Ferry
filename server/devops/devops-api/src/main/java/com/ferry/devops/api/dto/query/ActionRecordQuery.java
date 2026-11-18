package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 动作记录查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ActionRecordQuery extends Query {

    @Schema(description = "动作标识")
    private String actionId;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "级别")
    private Integer level;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "来源")
    private Integer source;
}
