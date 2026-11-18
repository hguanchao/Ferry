package com.ferry.devops.api.vo;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 应用 AI 授权 VO
 */
@Data
public class AppAiLevelVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "已开通最高级别")
    private Integer level;

    @Schema(description = "授权人")
    private Long grantedBy;

    @Schema(description = "授权时间")
    private Date grantedAt;
}
