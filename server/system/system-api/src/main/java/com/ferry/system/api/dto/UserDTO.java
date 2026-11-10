package com.ferry.system.api.dto;

import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 用户保存入参。租户不在此接收：按登录态由填充器写入，采信请求体的值会造成跨租户写人。
 */
@Data
public class UserDTO {

    @Schema(description = "用户id")
    private Long id;

    @Schema(description = "用户账号")
    @NotBlank(message = "账号不能为空")
    private String account;

    @Schema(description = "登录密码")
    @Size(min = 6, max = 32, message = "密码长度需在6到32位之间")
    private String password;

    @Schema(description = "昵称")
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "生日")
    @DateTimeFormat(pattern = DateUtil.NORM_DATE_PATTERN)
    private Date birthday;

    @Schema(description = "性别 1：男 2：女")
    private Integer gender;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "账号状态 0：禁用 1：正常")
    private Integer status;

    @Schema(description = "所属部门id")
    private String deptId;

    @Schema(description = "在职岗位id")
    private String postId;

    @Schema(description = "授权角色id")
    private String roleId;
}