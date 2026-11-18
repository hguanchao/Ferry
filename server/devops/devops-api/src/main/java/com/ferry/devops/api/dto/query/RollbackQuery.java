package com.ferry.devops.api.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import com.ferry.common.jdbc.provider.Query;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 回滚记录查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RollbackQuery extends Query {

    @Schema(description = "环境id")
    private Long envId;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "结果")
    private Integer result;

    @Schema(description = "开始时间起")
    private String startTimeStart;

    @Schema(description = "开始时间止")
    private String startTimeEnd;
}
