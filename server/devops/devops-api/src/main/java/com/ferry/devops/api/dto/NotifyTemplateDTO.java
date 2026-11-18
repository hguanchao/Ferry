package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通知模板表单
 */
@Data
public class NotifyTemplateDTO {

    @Schema(description = "主键(编辑时传)")
    private Long id;

    @Schema(description = "事件")
    @NotBlank(message = "事件不能为空")
    private String event;

    @Schema(description = "渠道类型")
    @NotNull(message = "渠道类型不能为空")
    private Integer channelType;

    @Schema(description = "标题模板")
    @NotBlank(message = "标题模板不能为空")
    private String titleTemplate;

    @Schema(description = "正文模板")
    @NotBlank(message = "正文模板不能为空")
    private String bodyTemplate;

    @Schema(description = "变量清单(逗号分隔)")
    private String variables;

    @Schema(description = "启停")
    private Integer isEnabled;
}
