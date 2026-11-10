package com.ferry.auth.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求
 *
 * <p>凭据统一走请求体，避免密码出现在 URL、代理日志与浏览器历史中。
 */
@Data
public class LoginDTO {

    @Schema(description = "授权类型", example = "password")
    @NotBlank(message = "授权类型不能为空")
    private String grantType;

    @Schema(description = "账号", example = "admin")
    @NotBlank(message = "账号不能为空")
    private String account;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "租户id")
    private String tenantId;
}
