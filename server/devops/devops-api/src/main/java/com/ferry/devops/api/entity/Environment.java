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
 * 环境档案
 */
@Data
@TableName(value = "dev_environment")
@EqualsAndHashCode(callSuper = true)
public class Environment extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 应用id */
    private Long appId;

    /** 环境名 */
    private String name;

    /** 环境类别 1测试 2预发 3生产 */
    private Integer category;

    /** 部署类型 1SSH 2Docker 3Kubernetes */
    private Integer deployType;

    /** 保护标记，生产自动置1 */
    private Integer isProtected;

    /** 目标资源JSON */
    private String targetConfig;

    /** 健康检查配置JSON */
    private String healthConfig;

    /** 观察时长分钟 */
    private Integer observeMinutes;

    /** 配置版本 */
    private Integer configVersion;

    /** 当前生效制品id */
    private Long currentArtifactId;

    /** 状态 1正常 2异常；类型变更申请时2表示待审批 */
    private Integer status;
}