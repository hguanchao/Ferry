package com.ferry.system.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@TableName(value = "sys_tenant")
@EqualsAndHashCode(callSuper = true)
public class Tenant extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 租户编码
     */
    private String code;

    /**
     * 租户名称
     */
    private String name;

    /**
     * 租户状态 0：禁用 1：正常
     */
    private Integer status;
}