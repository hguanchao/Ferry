package com.ferry.devops.api.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动作记录 VO
 */
@Data
public class ActionRecordVO {

    @Schema(description = "记录id")
    private Long id;

    @Schema(description = "动作标识")
    private String actionId;

    @Schema(description = "级别")
    private Integer level;

    @Schema(description = "来源 1 归因 2 预案 3 异常")
    private Integer source;

    @Schema(description = "业务id")
    private String bizId;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "环境id")
    private Long envId;

    @Schema(description = "理由")
    private String reason;

    @Schema(description = "执行人")
    private Long operator;

    @Schema(description = "状态 1-8")
    private Integer status;

    @Schema(description = "执行时间")
    private Date executeTime;

    @Schema(description = "撤销截止")
    private Date undoDeadline;

    @Schema(description = "撤销时间")
    private Date undoTime;

    @Schema(description = "撤销人")
    private Long undoUser;

    @Schema(description = "结果")
    private String result;

    @Schema(description = "失败原因")
    private String error;

    @Schema(description = "创建时间")
    private Date createTime;
}
