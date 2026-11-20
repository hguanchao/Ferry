package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流水线脚本预览结果
 */
@Data
public class PipelinePreviewVO {

    @Schema(description = "按当前定义渲染的 Jenkins 脚本预览")
    private String script;

    @Schema(description = "触发时实际下发给 Jenkins 的 STAGE_SPEC")
    private String stageSpec;

    @Schema(description = "预览告警：空命令、缺少产出物等")
    private List<String> warnings;
}
