package com.ferry.system.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MenuDTO {

    @Schema(description = "菜单id")
    private Long id;

    @Schema(description = "父菜单id")
    private Long parentId;

    @Schema(description = "菜单名称")
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    @Schema(description = "菜单类型 0：目录 1：菜单 2：按钮")
    @NotNull(message = "菜单类型不能为空")
    private Integer type;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "是否显示 0：隐藏 1：显示")
    private Integer isShow;

    @Schema(description = "是否为外链 0：否 1：是")
    private Integer isFrame;

    @Schema(description = "是否缓存 0：不缓存 1：缓存")
    private Integer isCache;

    @Schema(description = "菜单状态 0：停用 1：正常")
    private Integer status;

    @Schema(description = "显示顺序")
    private Integer sort;
}