package com.ferry.system.api.vo.tree;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.model.node.TreeNode;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class MenuTree extends TreeNode {

    @Schema(description = "租户编码")
    private String tenantId;

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

    @Schema(description = "菜单状态 0：停用 1：正常")
    private Integer status;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}