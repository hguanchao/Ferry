package com.ferry.system.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 行政区划
 *
 * <p>国标数据，各租户共用同一份，因此不设租户字段，也不参与租户拦截。
 */
@Data
@TableName(value = "sys_region")
@EqualsAndHashCode(callSuper = true)
public class Region extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 父级区域ID
     */
    private Long parentId;

    /**
     * 区域编码
     */
    private String code;

    /**
     * 区域名称
     */
    private String name;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态 0：禁用 1：正常
     */
    private Integer status;
}
