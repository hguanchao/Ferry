package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 运行
 */
@Data
@TableName(value = "dev_run")
@EqualsAndHashCode(callSuper = true)
public class Run extends BaseEntity implements Serializable {

    /** 主键id */
    @TableId
    private Long id;

    /** 应用id */
    private Long appId;

    /** 流水线id */
    private Long pipelineId;

    /** 运行编号 RUN-yyyyMMdd-序号 */
    private String code;

    /** 分支 */
    private String branch;

    /** 引用类型 1=Commit 2=Tag/Ref */
    private Integer refType;

    /** 引用值（触发输入或应用登记的默认值） */
    private String refValue;

    /** 触发类型 1=手动 2=自动 */
    private Integer triggerType;

    /** 触发人id */
    private Long triggerUser;

    /** 触发说明 */
    private String triggerNote;

    /** 状态 1=排队中 2=运行中 3=阻塞 4=成功 5=失败 6=超时 7=已取消 8=结果待确认 */
    private Integer status;

    /** 流水线版本号 */
    private Integer pipelineVersion;

    /** Jenkins任务id */
    private String jenkinsJobId;

    /** Jenkins构建编号 */
    private Long jenkinsBuildNo;

    /** Jenkins队列项id */
    private Long jenkinsQueueId;

    /** 预检状态 1=正常 2=异常 */
    private Integer preCheckStatus;

    /** 预检内容 */
    private String preCheckContent;

    /** 开始时间 */
    private Date startTime;

    /** 结束时间 */
    private Date endTime;
}
