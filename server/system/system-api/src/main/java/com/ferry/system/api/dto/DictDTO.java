package com.ferry.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DictDTO {

    @Schema(description = "字典id")
    private Long id;

    @Schema(description = "父字典id")
    private Long parentId;

    @Schema(description = "字典码")
    @NotBlank(message = "字典码不能为空")
    private String code;

    @Schema(description = "字典标签")
    @NotBlank(message = "字典标签不能为空")
    private String label;

    @Schema(description = "字典值")
    private String value;

    @Schema(description = "排序")
    private Integer sort;
}