package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;

/**
 * 发布单
 */
@Data
@TableName("dev_release_candidate")
@EqualsAndHashCode(callSuper = true)
public class ReleaseCandidate extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 租户编码 */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /** 发布单号 */
    private String rcNo;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 状态 1草稿 2待审批 3已批准 4发布中 5观察中 6已完成 7已回滚 8已驳回 9已取消 */
    private Integer status;

    /** 风险等级 1低 2中 3高 */
    private Integer riskLevel;

    /** 加权分 */
    private java.math.BigDecimal riskScore;

    /** 五因子明细JSON */
    private String riskDetail;

    /** 三层影响面 */
    private String impactJson;

    /** 计划窗口开始 */
    private Date windowStart;

    /** 计划窗口结束 */
    private Date windowEnd;

    /** 变更单id */
    private Long changeTicketId;

    /** 创建人 */
    private Long creator;

    /** 提交时间 */
    private Date submittedAt;

    /** 关闭时间 */
    private Date closedAt;
}
