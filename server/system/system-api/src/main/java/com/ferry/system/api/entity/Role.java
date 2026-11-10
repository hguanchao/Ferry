package com.ferry.system.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@TableName(value = "sys_role")
@EqualsAndHashCode(callSuper = true)
public class Role extends BaseEntity implements Serializable {

    /**
     * 数据id
     */
    @TableId
    private Long id;

    /**
     * 租户编码
     */
    private String tenantId;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色状态 0：停用 1：正常
     */
    private Integer status;

    /**
     * 数据范围 1：全部 2：自定义 3：本部门 4：本部门及以下 5：仅本人
     */
    private Integer dataScope;

    /**
     * 显示顺序
     */
    private Integer sort;
}