package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PipelineStageTaskVO {

    @Schema(name = "数据id")
    private Long id;

    @Schema(name = "任务名称")
    private String name;

    @Schema(name = "执行器", description = "1jenkins 2deployer 3gate 4release")
    private Integer executor;

    @Schema(name = "源码脚本路径")
    private String scriptRef;

    @Schema(name = "自定义脚本内容")
    private String scriptContent;

    @Schema(name = "脚本参数")
    private String scriptParams;

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "阶段id")
    private Long stageId;

    @Schema(name = "排序号")
    private Integer sort;
}
