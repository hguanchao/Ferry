package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通知模板 VO
 */
@Data
public class NotifyTemplateVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "事件")
    private String event;

    @Schema(description = "渠道类型")
    private Integer channelType;

    @Schema(description = "标题模板")
    private String titleTemplate;

    @Schema(description = "正文模板")
    private String bodyTemplate;

    @Schema(description = "变量清单")
    private String variables;

    @Schema(description = "启停")
    private Integer isEnabled;
}
