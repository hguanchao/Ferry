package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 存量基线。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_gate_baseline")
public class GateBaseline implements Serializable {

    @TableId
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 应用id */
    private Long appId;

    /** 扫描工具 */
    private String tool;

    /** 规则版本 */
    private String ruleVersion;

    /** 问题总数 */
    private Integer totalCount;

    /** 各级别数量 JSON */
    private String severityStats;

    /** 指纹算法版本 */
    private String fingerprintVersion;

    /** 是否生效 */
    private Integer isActive;

    /** 创建人 */
    private Long createUser;

    /** 创建时间 */
    private Date createTime;
}
