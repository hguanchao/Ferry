package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门禁豁免查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GateExemptionQuery extends Query {

    @Schema(description = "运行id")
    private Long runId;

    @Schema(description = "运行阶段门禁结果id")
    private Long runStageGateId;

    @Schema(description = "状态 1 生效 2 已到期 3 已撤销")
    private Integer status;
}
