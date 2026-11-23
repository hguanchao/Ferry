package com.ferry.devops.api.vo;

import com.ferry.devops.api.entity.PipelineStageTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 定义阶段 VO
 */
@Data
public class PipelineStageVO {

    @Schema(name = "数据id")
    private Long id;

    @Schema(name = "阶段类型", description = "1构建 2单测 3静态扫描 4制品入库 5部署非生产 6集成测试 7准入 8生产发布")
    private Integer type;

    @Schema(name = "阶段名称")
    private String name;

    @Schema(name = "构建工具", description = "Maven/Node/Gradle")
    private String buildTool;

    @Schema(name = "版本号")
    private Integer version;

    @Schema(name = "超时秒数")
    private Integer timeout;

    @Schema(name = "产出物")
    private String outputs;

    @Schema(name = "流水线id")
    private Long pipelineId;

    @Schema(name = "排序号")
    private Integer sort;

    @Schema(name = "阶段任务列表")
    private List<PipelineStageTaskVO> taskList;
}
