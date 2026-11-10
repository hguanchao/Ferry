package com.ferry.system.api.vo;

import com.ferry.common.core.utils.DateUtil;
import com.ferry.system.api.vo.tree.MenuTree;
import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class UserVO implements Serializable {

    @Schema(description = "用户id")
    @JsonSerialize(using = ToStringSerializer.class)
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

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号码")
    private String phone;

    @Schema(description = "生日")
    @JsonFormat(pattern = DateUtil.NORM_DATE_PATTERN)
    private Date birthday;

    @Schema(description = "性别 1：男 2：女")
    private Integer gender;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "超级管理员 0：否 1：是")
    private Integer isSuper;

    @Schema(description = "账号状态 0：禁用 1：正常")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;

    @Schema(description = "所属部门id")
    private String deptId;

    @Schema(description = "在职岗位id")
    private String postId;

    @Schema(description = "用户角色id")
    private String roleId;
}