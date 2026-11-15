package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 触发记录查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RunTriggerRecordQuery extends Query {

    @Schema(description = "应用id")
    private Long appId;
}
