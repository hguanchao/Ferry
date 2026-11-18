package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 审批记录
 */
@Data
@TableName("dev_release_approval")
public class ReleaseApproval implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    private String tenantId;

    /** 发布单id */
    private Long rcId;

    /** 审批节点 */
    private Integer node;

    /** 审批人 */
    private Long approver;

    /** 意见 */
    private String opinion;

    /** 结果 1通过 2驳回 */
    private Integer result;

    /** 时间 */
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
