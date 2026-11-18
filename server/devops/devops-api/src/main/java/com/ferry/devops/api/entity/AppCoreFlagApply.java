package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 核心链路变更申请
 */
@Data
@TableName(value = "dev_app_core_flag_apply")
@EqualsAndHashCode(callSuper = true)
public class AppCoreFlagApply extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 应用id */
    private Long appId;

    /** 申请的目标值 */
    private Integer isTargetCore;

    /** 申请理由 */
    private String reason;

    /** 1 待审批 2 已通过 3 已驳回 */
    private Integer status;

    /** 申请人 */
    private Long applicant;

    /** 审批人 */
    private Long approver;

    /** 审批时间 */
    private java.util.Date approveTime;
}