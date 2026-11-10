package com.ferry.system.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName(value = "sys_user")
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 租户编码
     */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /**
     * 用户账号
     */
    private String account;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 性别 1：男 2：女
     */
    private Integer gender;

    /**
     * 地址
     */
    private String address;

    /**
     * 超级管理员 0：否 1：是
     */
    private Integer isSuper;

    /**
     * 账号状态 0：禁用 1：正常
     */
    private Integer status;

    /**
     * 所属部门id
     */
    private String deptId;

    /**
     * 在职岗位id
     */
    private String postId;

    /**
     * 第三方认证id
     */
    private String oauthId;
}