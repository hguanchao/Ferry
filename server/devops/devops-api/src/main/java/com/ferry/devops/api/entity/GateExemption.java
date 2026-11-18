package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 门禁豁免。表无 is_deleted 字段，不继承 BaseEntity。
 */
@Data
@TableName(value = "dev_gate_exemption")
public class GateExemption implements Serializable {

    @TableId
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 结果id */
    private Long gateResultId;

    /** 运行id */
    private Long runId;

    /** 规则id */
    private Long ruleId;

    /** 理由，不少于 20 字 */
    private String reason;

    /** 到期时间 */
    private Date expireAt;

    /** 关联问题单 */
    private String issueNo;

    /** 申请人 */
    private Long applicant;

    /** 1 生效 2 已到期 3 已撤销 */
    private Integer status;

    /** 申请时间 */
    private Date createTime;
}
