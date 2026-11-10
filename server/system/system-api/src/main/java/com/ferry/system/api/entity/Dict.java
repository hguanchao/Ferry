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
@TableName(value = "sys_dict")
@EqualsAndHashCode(callSuper = true)
public class Dict extends BaseEntity implements Serializable {

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
     * 父字典id
     */
    private Long parentId;

    /**
     * 字典码
     */
    private String code;

    /**
     * 字典标签
     */
    private String label;

    /**
     * 字典值
     */
    private String value;

    /**
     * 显示顺序
     */
    private Integer sort;
}