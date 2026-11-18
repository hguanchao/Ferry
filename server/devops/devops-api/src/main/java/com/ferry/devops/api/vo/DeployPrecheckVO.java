package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部署校验项结果
 */
@Data
public class DeployPrecheckVO {

    @Schema(description = "校验项类型")
    private Integer type;

    @Schema(description = "校验项名称")
    private String typeName;

    @Schema(description = "结论 1通过 2不通过")
    private Integer conclusion;

    @Schema(description = "明细")
    private String detail;
}
