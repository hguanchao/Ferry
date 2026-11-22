package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 运行阶段任务 VO
 */
@Data
public class RunStageTaskVO {

    @Schema(description = "任务id")
    private Long id;

    @Schema(description = "运行id")
    private Long runId;

    @Schema(description = "运行阶段id")
    private Long runStageId;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "执行器")
    private String executor;

    @Schema(description = "源码脚本路径")
    private String scriptRef;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "运行结果信息(JSON)")
    private String result;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "结束时间")
    private Date endTime;

    @Schema(description = "创建时间")
    private Date createTime;
}
