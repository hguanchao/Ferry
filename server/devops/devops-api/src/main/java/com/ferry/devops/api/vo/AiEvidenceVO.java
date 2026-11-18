package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 证据 VO
 */
@Data
public class AiEvidenceVO {

    @Schema(description = "证据id")
    private Long id;

    @Schema(description = "类型 1 日志 2 指标 3 变更 4 历史")
    private Integer type;

    @Schema(description = "定位")
    private String locator;

    @Schema(description = "原文片段")
    private String snippet;

    @Schema(description = "校验是否通过")
    private Integer isValid;

    @Schema(description = "顺序")
    private Integer sort;
}
