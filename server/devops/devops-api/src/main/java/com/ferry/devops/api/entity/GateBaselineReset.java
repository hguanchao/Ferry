package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 基线重设申请。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_gate_baseline_reset")
public class GateBaselineReset implements Serializable {

    @TableId
    private Long id;

    /** 应用id */
    private Long appId;

    /** 原基线 */
    private Long oldBaselineId;

    /** 新基线 */
    private Long newBaselineId;

    /** 理由 */
    private String reason;

    /** 审批人 */
    private Long approver;

    /** 1 待审批 2 已通过 3 已驳回 */
    private Integer status;

    /** 申请时间 */
    private Date createTime;
}
