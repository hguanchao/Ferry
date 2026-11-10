package com.ferry.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ConfigDTO {

    @Schema(description = "参数id")
    private Long id;

    @Schema(description = "参数名称")
    private String name;

    @Schema(description = "参数键名")
    private String code;

    @Schema(description = "参数键值")
    private String value;

    @Schema(description = "是否系统内置 0：否 1：是")
    private Integer isInternal;
}
