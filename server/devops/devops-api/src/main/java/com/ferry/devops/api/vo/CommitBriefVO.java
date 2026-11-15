package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CommitBriefVO {

    @Schema(description = "提交id（完整 sha）")
    private String sha;

    @Schema(description = "提交人")
    private String author;

    @Schema(description = "提交时间（ISO-8601 文本）")
    private String committedTime;

    @Schema(description = "提交说明")
    private String message;
}
