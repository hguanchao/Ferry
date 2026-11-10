package com.ferry.common.security.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 认证用户信息类
 */
@Data
public class AuthUser implements Serializable {

    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "租户编码")
    private String tenantId;

    @Schema(description = "用户账号")
    private String account;

    @Schema(description = "用户密码")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "账号状态 0：禁用 1：正常")
    private Integer status;

    @Schema(description = "用户角色id")
    private String roleId;

    @Schema(description = "超级管理员 0：否 1：是")
    private Integer isSuper;

    @Schema(description = "用户角色权限列表")
    private List<AuthPerm> permissions;

    @Schema(description = "数据范围：是否可查看全部")
    private Boolean dataAll;

    @Schema(description = "数据范围：是否可查看本人创建的数据")
    private Boolean dataSelf;

    @Schema(description = "数据范围：可见部门id，已包含下级部门")
    private Set<Long> dataDeptIds;
}
