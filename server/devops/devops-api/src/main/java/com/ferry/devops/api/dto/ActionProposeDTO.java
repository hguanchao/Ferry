package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * AI 动作提议表单
 */
@Data
public class ActionProposeDTO {

    @Schema(description = "动作标识")
    @NotBlank(message = "动作标识不能为空")
    private String actionId;

    @Schema(description = "参数JSON")
    private String params;

    @Schema(description = "触发来源 1 归因 2 预案 3 异常")
    @NotNull(message = "触发来源不能为空")
    private Integer source;

    @Schema(description = "业务id")
    private String bizId;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "环境id")
    private Long envId;

    @Schema(description = "理由")
    private String reason;

    @Schema(description = "证据定位列表")
    private List<String> evidence;
}
