package com.ferry.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TenantDTO {

    @Schema(description = "租户id")
    private Long id;

    @Schema(description = "租户编码")
    private String code;

    @Schema(description = "租户名称")
    private String name;

    @Schema(description = "租户状态 0：禁用 1：正常")
    private Integer status;
}