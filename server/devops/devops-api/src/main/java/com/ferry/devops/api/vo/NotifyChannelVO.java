package com.ferry.devops.api.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通知渠道 VO
 */
@Data
public class NotifyChannelVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "渠道名")
    private String name;

    @Schema(description = "1 IM 2 邮件 3 Webhook")
    private Integer type;

    @Schema(description = "配置摘要(敏感项掩码)")
    private String configJson;

    @Schema(description = "1 正常 2 异常")
    private Integer status;

    @Schema(description = "最近发送")
    private Date lastSendTime;

    @Schema(description = "最近失败")
    private String lastError;
}
