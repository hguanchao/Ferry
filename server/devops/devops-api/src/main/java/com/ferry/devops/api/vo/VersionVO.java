package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 版本摘要：流水线定义已按阶段/任务表按版本号存储，无独立版本行，
 * 因此没有版本 id、来源与内容快照字段，仅描述版本号与变更说明。
 */
@Data
public class VersionVO {

    @Schema(description = "流水线id")
    private Long pipelineId;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "变更说明（保存/回退接口回显，版本列表暂不落库）")
    private String changeNote;
}
