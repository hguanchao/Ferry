package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开放 API Token
 */
@Data
@TableName("sys_open_token")
@EqualsAndHashCode(callSuper = true)
public class OpenToken extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 绑定应用 */
    private Long appId;

    /** Token 名称 */
    private String name;

    /** sha256 */
    private String tokenHash;

    /** 掩码 */
    private String tokenMask;

    /** 每分钟配额 */
    private Integer quotaPerMinute;

    /** 1 正常 2 禁用 */
    private Integer status;

    /** 过期时间 */
    private Date expireAt;

    /** 最近调用 */
    private Date lastUsedTime;

    /** 最近轮换 */
    private Date lastRotateTime;
}
