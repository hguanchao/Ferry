package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;

/**
 * 变更单
 */
@Data
@TableName("dev_change_ticket")
@EqualsAndHashCode(callSuper = true)
public class ChangeTicket extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 变更单号 */
    private String ticketNo;

    /** 类型 1常规 2紧急 3标准 */
    private Integer type;

    /** 标题 */
    private String title;

    /** 变更内容 */
    private String content;

    /** 计划窗口开始 */
    private Date windowStart;

    /** 计划窗口结束 */
    private Date windowEnd;

    /** 回滚方案 */
    private String rollbackPlan;

    /** 影响评估 */
    private String impactJson;

    /** 状态 1草稿 2待审批 3已批准 4已实施 5已关闭 6已取消 */
    private Integer status;

    /** 申请人 */
    private Long applicant;

    /** 审批人 */
    private Long approver;

    /** 来源 1本系统 2外部同步 */
    private Integer source;

    /** 外部单号 */
    private String externalNo;
}
