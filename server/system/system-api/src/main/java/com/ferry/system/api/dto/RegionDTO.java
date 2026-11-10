package com.ferry.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class RegionDTO implements Serializable {

    @Schema(description = "区域id")
    private Long id;

    @Schema(description = "父级id")
    private Long parentId;

    @Schema(description = "区域编码")
    private String code;

    @Schema(description = "区域名称")
    private String name;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态 0：禁用 1：正常")
    private Integer status;
}
