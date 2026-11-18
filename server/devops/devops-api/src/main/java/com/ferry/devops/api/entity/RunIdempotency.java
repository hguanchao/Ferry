package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 触发幂等（uk: tenant_id + idempotent_key）
 */
@Data
@TableName(value = "dev_run_idempotency")
public class RunIdempotency implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** sha256 幂等键 */
    private String idempotentKey;

    /** 应用id */
    private Long appId;

    /** 运行id */
    private Long runId;

    /** 1活动中 2已成功 3已失效 */
    private Integer state;

    /** 失效时间 */
    private java.util.Date expireTime;
}
