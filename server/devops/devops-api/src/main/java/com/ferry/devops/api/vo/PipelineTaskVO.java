package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 定义任务 VO
 */
@Data
public class PipelineTaskVO {

    @Schema(description = "任务名")
    private String name;

    @Schema(description = "执行器")
    private String executor;

    @Schema(description = "脚本引用")
    private String scriptRef;

    @Schema(description = "自定义脚本内容")
    private String scriptContent;

    @Schema(description = "参数")
    private Map<String, Object> params;
}
