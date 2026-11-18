package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 开放 API Token 签发表单
 */
@Data
public class OpenTokenDTO {

    @Schema(description = "绑定应用")
    @NotNull(message = "绑定应用不能为空")
    private Long appId;

    @Schema(description = "Token 名称")
    private String name;

    @Schema(description = "每分钟配额，默认 100")
    private Integer quotaPerMinute;
}
