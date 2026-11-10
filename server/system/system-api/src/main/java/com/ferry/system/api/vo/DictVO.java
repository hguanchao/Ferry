package com.ferry.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DictVO {

    @Schema(description = "字典id")
    private Long id;

    @Schema(description = "父字典id")
    private Long parentId;

    @Schema(description = "字典码")
    private String code;

    @Schema(description = "字典标签")
    private String label;

    @Schema(description = "字典值")
    private String value;

    @Schema(description = "显示排序")
    private Integer sort;
}