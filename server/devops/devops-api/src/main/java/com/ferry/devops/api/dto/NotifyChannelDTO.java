package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通知渠道表单
 */
@Data
public class NotifyChannelDTO {

    @Schema(description = "主键(编辑时传)")
    private Long id;

    @Schema(description = "渠道名")
    @NotBlank(message = "渠道名不能为空")
    private String name;

    @Schema(description = "1 IM 2 邮件 3 Webhook")
    @NotNull(message = "渠道类型不能为空")
    private Integer type;

    @Schema(description = "连接配置JSON(敏感项加密)")
    private String configJson;

    @Schema(description = "1 正常 2 异常")
    private Integer status;
}
