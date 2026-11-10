package com.ferry.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PostDTO {

    @Schema(description = "岗位id")
    private Long id;

    @Schema(description = "租户编码")
    private String tenantId;

    @Schema(description = "岗位编码")
    private String code;

    @Schema(description = "岗位名称")
    private String name;

    @Schema(description = "岗位状态 0：停用 1：正常")
    private Integer status;

    @Schema(description = "显示顺序")
    private Integer sort;
}