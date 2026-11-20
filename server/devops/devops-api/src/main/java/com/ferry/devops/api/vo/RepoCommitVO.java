package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 触发弹窗提交候选
 */
@Data
public class RepoCommitVO {

    @Schema(description = "完整提交 SHA")
    private String sha;

    @Schema(description = "短 SHA")
    private String shortSha;

    @Schema(description = "提交说明（首行）")
    private String message;

    @Schema(description = "提交人")
    private String authorName;

    @Schema(description = "提交时间（ISO 8601）")
    private String committedAt;
}
