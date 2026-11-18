package com.ferry.devops.api.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ferry.common.jdbc.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 动作记录
 */
@Data
@TableName("dev_action_record")
@EqualsAndHashCode(callSuper = true)
public class ActionRecord extends BaseEntity implements Serializable {

    /** 数据id */
    @TableId
    private Long id;

    /** 动作标识 */
    private String actionId;

    /** 级别 */
    private Integer level;

    /** 1 归因 2 预案 3 异常 */
    private Integer source;

    /** 业务id */
    private String bizId;

    /** 应用id */
    private Long appId;

    /** 环境id */
    private Long envId;

    /** 参数JSON */
    private String paramsJson;

    /** 理由 */
    private String reason;

    /** 证据JSON */
    private String evidenceJson;

    /** 执行人 */
    private Long operator;

    /** 模型版本 */
    private String modelVersion;

    /** 提示版本 */
    private String promptVersion;

    /** 1-8 动作状态 */
    private Integer status;

    /** 执行时间 */
    private Date executeTime;

    /** 撤销截止 */
    private Date undoDeadline;

    /** 撤销时间 */
    private Date undoTime;

    /** 撤销人 */
    private Long undoUser;

    /** 结果 */
    private String result;

    /** 失败原因 */
    private String error;
}
