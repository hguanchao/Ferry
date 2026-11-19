package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流水线
 */
@Data
@TableName(value = "dev_pipeline")
@EqualsAndHashCode(callSuper = true)
public class Pipeline extends BaseEntity implements Serializable {

    /** 主键id */
    @TableId
    private Long id;

    /** 流水线名称 */
    private String name;

    /** 状态 0停用 1启用 */
    private Integer status;

    /** 应用id */
    private Long appId;

    /** 当前版本号 */
    private Integer version;
}
