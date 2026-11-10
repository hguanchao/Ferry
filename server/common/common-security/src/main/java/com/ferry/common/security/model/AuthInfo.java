package com.ferry.common.security.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 认证信息类
 */
@Data
public class AuthInfo implements Serializable {

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "租户编码")
    private String tenantId;

    @Schema(description = "用户账号")
    private String account;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "超级管理员 0：否 1：是")
    private Integer isSuper;

    @Schema(description = "认证请求头名称")
    private String tokenHeader;

    @Schema(description = "访问令牌前缀")
    private String tokenPrefix;

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "用户角色权限列表")
    private List<AuthPerm> permissions;
}
