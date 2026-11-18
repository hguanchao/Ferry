package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 应用选项
 */
@Data
public class AppOptionVO {

    @Schema(description = "应用id")
    private Long value;

    @Schema(description = "应用名称")
    private String label;

    @Schema(description = "应用编码")
    private String code;
}