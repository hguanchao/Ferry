package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 运行阶段 VO
 */
@Data
public class RunStageVO {

    @Schema(description = "阶段id")
    private Long id;

    @Schema(description = "运行id")
    private Long runId;

    @Schema(description = "阶段类型 1=构建 2=单元测试 3=静态扫描 4=制品入库 5=部署非生产 6=集成测试 7=准入 8=生产发布")
    private Integer type;

    @Schema(description = "阶段名称")
    private String name;

    @Schema(description = "状态 1=未开始 2=进行中 3=成功 4=失败 5=阻塞 6=跳过 7=超时 8=已复用 9=已取消 10=待门禁")
    private Integer status;

    @Schema(description = "运行结果信息(JSON)")
    private String result;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "该阶段下的任务")
    private List<RunStageTaskVO> tasks;

    @Schema(description = "断点重跑计划：该阶段是否可复用")
    private Boolean reusable;
}
