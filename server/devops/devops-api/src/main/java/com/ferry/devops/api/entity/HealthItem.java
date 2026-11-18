package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

/**
 * 健康检查项配置
 */
@Data
@TableName("dev_health_item")
public class HealthItem implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    private String tenantId;

    /** 环境id */
    private Long envId;

    /** 检查项 1存活 2成功率 3P95 4错误日志 5核心指标 */
    private Integer checkType;

    /** 阈值 */
    private String threshold;

    /** 采样间隔秒 */
    private Integer sampleInterval;

    /** 连续命中次数 */
    private Integer consecutive;

    /** 是否支持 */
    private Integer isSupported;
}
