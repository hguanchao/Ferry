package com.ferry.devops.api.entity;
import java.util.Date;

import java.io.Serializable;

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

    /** 数据id */
    @TableId
    private Long id;

    /** 运行编号 RUN-yyyyMMdd-序号 */
    private String runNo;

    /** 流水线id */
    private Long pipelineId;

    /** 版本id */
    private Long pipelineVersionId;

    /** 应用id */
    private Long appId;

    /** 分支 */
    private String branch;

    /** 提交 */
    private String commitId;

    /** 1 Commit 2 Tag/Ref */
    private Integer refType;

    /** 原始引用值 */
    private String refValue;

    /** 1 手动（历史值保留） */
    private Integer triggerType;

    /** 触发人 */
    private Long triggerUser;

    /** 触发说明 */
    private String triggerNote;

    /** 1排队 2运行中 3阻塞 4成功 5失败 6超时 7已取消 8结果待确认 */
    private Integer status;

    /** 1门禁不通过 2门禁数据获取失败 3人工暂停 */
    private Integer blockReason;

    /** 预校验是否通过 */
    private Integer isPrecheckPass;

    /** 幂等键 */
    private String idempotentKey;

    /** 重跑自 */
    private Long rerunFromRunId;

    /** 复用自 */
    private Long reusedRunId;

    /** 产出制品 */
    private Long artifactId;

    /** Jenkins 构建号 */
    private Long jenkinsBuildNo;

    /** 结果待确认截止时间 */
    private java.util.Date resultExpireTime;

    /** 开始时间 */
    private java.util.Date startTime;

    /** 结束时间 */
    private java.util.Date endTime;

    /** 耗时毫秒 */
    private Long duration;
}
