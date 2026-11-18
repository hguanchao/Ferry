package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 依赖服务
 */
@Data
@TableName(value = "dev_app_dependency")
@EqualsAndHashCode(callSuper = true)
public class AppDependency extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 应用id */
    private Long appId;

    /** 依赖服务名 */
    private String depName;

    /** 关联应用id */
    private Long depAppId;

    /** 版本范围 */
    private String versionRange;
}