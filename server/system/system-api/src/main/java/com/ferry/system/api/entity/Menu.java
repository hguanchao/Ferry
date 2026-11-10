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
@TableName(value = "sys_menu")
@EqualsAndHashCode(callSuper = true)
public class Menu extends BaseEntity implements Serializable {

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
     * 父菜单id
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单类型 0：目录 1：菜单 2：按钮
     */
    private Integer type;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 权限标识
     */
    private String perms;

    /**
     * 是否显示 0：隐藏 1：显示
     */
    private Integer isShow;

    /**
     * 是否为外链 0：否 1：是
     */
    private Integer isFrame;

    /**
     * 是否缓存 0：不缓存 1：缓存
     */
    private Integer isCache;

    /**
     * 菜单状态 0：停用 1：正常
     */
    private Integer status;

    /**
     * 显示顺序
     */
    private Integer sort;
}