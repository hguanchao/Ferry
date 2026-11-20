package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 流水线脚本预览结果
 */
@Data
public class PipelinePreviewVO {

    @Schema(description = "按当前定义渲染的完整 Jenkins 声明式脚本，与 Job 同步脚本同源")
    private String script;

    @Schema(description = "预览告警：空命令、缺少产出物等")
    private List<String> warnings;
}
