package com.ferry.common.security.model;

import com.ferry.common.core.model.node.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 认证用户权限
 *
 * <p>登录后随认证信息下发，供前端渲染菜单与按钮级权限标识。
 * 放在公共模块是为了让认证信息不反向依赖业务服务的视图对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthPerm extends TreeNode {

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "菜单类型 0：目录 1：菜单 2：按钮")
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

    @Schema(description = "显示顺序")
    private Integer sort;
}
