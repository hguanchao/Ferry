package com.ferry.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
public class RoleDTO {

    @Schema(description = "角色id")
    private Long id;

    @Schema(description = "角色编码")
    @NotBlank(message = "角色编码不能为空")
    private String code;

    @Schema(description = "角色名称")
    @NotBlank(message = "角色名称不能为空")
    private String name;

    @Schema(description = "角色状态 0：停用 1：正常")
    private Integer status;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "职责描述")
    @Size(max = 255, message = "职责描述不能超过 255 个字符")
    private String remark;
}