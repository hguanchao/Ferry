package com.ferry.devops.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 环境配置
 */
@Data
@TableName(value = "dev_env_config")
@EqualsAndHashCode(callSuper = true)
public class EnvConfig extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 环境id */
    private Long envId;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 值类型 1普通 2敏感 */
    private Integer valueType;

    /** 配置版本 */
    private Integer version;

    /** 是否加密 */
    private Integer isEncrypted;
}