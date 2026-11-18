package com.ferry.devops.api.vo;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 制品追溯：摘要 → commit → 人 → 发布单 → 部署/回滚。
 */
@Data
public class TraceVO {

    @Schema(description = "制品id")
    private Long artifactId;

    @Schema(description = "摘要")
    private String digest;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "分支")
    private String branch;

    @Schema(description = "提交")
    private String commitId;

    @Schema(description = "构建人")
    private Long builder;

    @Schema(description = "产出运行id")
    private Long runId;

    @Schema(description = "关联发布单id列表")
    private List<Long> releaseIds;

    @Schema(description = "关联部署记录id列表")
    private List<Long> deployIds;

    @Schema(description = "关联回滚记录id列表")
    private List<Long> rollbackIds;
}
