package com.ferry.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DeptDTO {

    @Schema(description = "部门id")
    private Long id;

    @Schema(description = "父部门id")
    private Long parentId;

    @Schema(description = "部门编码")
    private String code;

    @Schema(description = "部门名称")
    private String name;

    @Schema(description = "部门状态 0：禁用 1：正常")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;
}