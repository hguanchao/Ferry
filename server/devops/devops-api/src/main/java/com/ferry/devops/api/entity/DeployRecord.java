package com.ferry.devops.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;

/**
 * 部署记录
 */
@Data
@TableName("dev_deploy_record")
@EqualsAndHashCode(callSuper = true)
public class DeployRecord extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 部署编号 */
    private String deployNo;

    /** 应用id */
    private Long appId;

    /** 环境id */
    private Long envId;

    /** 执行器类型 */
    private Integer deployType;

    /** 制品id */
    private Long artifactId;

    /** 发布单id */
    private Long releaseId;

    /** 批次计划JSON */
    private String batchPlan;

    /** 校验快照 */
    private String precheckSnapshot;

    /** 状态 1未开始 2执行中 3观察中 4成功 5失败 6已回滚 7已取消 */
    private Integer status;

    /** 操作人 */
    private Long operator;

    /** 确认人 */
    private Long confirmer;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;

    /** 耗时毫秒 */
    private Long duration;

    /** 失败原因 */
    private String failReason;
}
