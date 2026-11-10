package com.ferry.system.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@TableName(value = "sys_post")
@EqualsAndHashCode(callSuper = true)
public class Post extends BaseEntity implements Serializable {

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
     * 岗位编码
     */
    private String code;

    /**
     * 岗位名称
     */
    private String name;

    /**
     * 岗位状态 0：停用 1：正常
     */
    private Integer status;

    /**
     * 显示顺序
     */
    private Integer sort;
}