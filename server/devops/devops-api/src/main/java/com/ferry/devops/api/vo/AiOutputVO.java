package com.ferry.devops.api.vo;

import java.util.Date;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 输出 VO
 */
@Data
public class AiOutputVO {

    @Schema(description = "输出id")
    private Long id;

    @Schema(description = "类型")
    private Integer type;

    @Schema(description = "业务id")
    private String bizId;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "根因类别")
    private String category;

    @Schema(description = "结论")
    private String conclusion;

    @Schema(description = "置信度 1 高 2 中 3 低")
    private Integer confidence;

    @Schema(description = "建议JSON")
    private String suggestionJson;

    @Schema(description = "状态 1 成功 2 丢弃 3 超时 4 无法归因")
    private Integer status;

    @Schema(description = "证据列表")
    private List<AiEvidenceVO> evidence;

    @Schema(description = "生成时间")
    private Date createTime;
}
