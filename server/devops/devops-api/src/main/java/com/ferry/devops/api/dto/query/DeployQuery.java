package com.ferry.devops.api.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import com.ferry.common.jdbc.provider.Query;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部署记录查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeployQuery extends Query {

    @Schema(description = "部署编号")
    private String deployNo;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "环境id")
    private Long envId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "部署类型")
    private Integer deployType;

    @Schema(description = "开始时间起")
    private String startTimeStart;

    @Schema(description = "开始时间止")
    private String startTimeEnd;
}
