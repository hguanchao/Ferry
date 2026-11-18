package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开放 API 调用明细
 */
@Data
@TableName("sys_open_api_log")
@EqualsAndHashCode(callSuper = true)
public class OpenApiLog extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** Token */
    private Long tokenId;

    /** 应用 */
    private Long appId;

    /** 接口 */
    private String path;

    /** 调用方 IP */
    private String ip;

    /** 响应码 */
    private Integer responseCode;

    /** 耗时 */
    private Long duration;
}
