package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 动作执行/确认表单
 */
@Data
public class ActionExecuteDTO {

    @Schema(description = "动作记录id")
    @NotNull(message = "动作记录id不能为空")
    private Long recordId;

    @Schema(description = "是否已确认影响面(L2 必填)")
    private Boolean impactConfirmed;
}
