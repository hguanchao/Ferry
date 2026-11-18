package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 环境选项
 */
@Data
public class EnvOptionVO {

    @Schema(description = "环境id")
    private Long value;

    @Schema(description = "环境名称")
    private String label;

    @Schema(description = "环境类别说明")
    private String code;
}