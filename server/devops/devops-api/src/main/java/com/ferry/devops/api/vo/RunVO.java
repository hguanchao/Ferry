package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 运行概要
 */
@Data
public class RunVO {

    @Schema(name = "数据id")
    private Long id;

    @Schema(name = "运行编号")
    private String code;

    @Schema(name = "分支")
    private String branch;

    @Schema(name = "引用类型", description = "1=Commit 2=Tag/Ref")
    private Integer refType;

    @Schema(name = "引用值")
    private String refValue;

    @Schema(name = "触发人")
    private Long triggerUser;

    @Schema(name = "触发说明")
    private String triggerNote;

    @Schema(name = "状态", description = "0=待确认 1=排队中 2=运行中 3=阻塞 4=成功 5=失败 6=超时 7=已取消 8=预检未通过")
    private Integer status;

    @Schema(name = "流水线id")
    private Long pipelineId;

    @Schema(name = "流水线名称")
    private String pipelineName;

    @Schema(name = "流水线版本号")
    private Integer pipelineVersionNo;

    @Schema(name = "应用id")
    private Long appId;

    @Schema(name = "应用名称")
    private String appName;

    @Schema(name = "预检结果")
    private String preCheckResult;

    @Schema(name = "Jenkins任务id")
    private String jenkinsJobId;

    @Schema(name = "Jenkins构建号")
    private Long jenkinsBuildNo;

    @Schema(name = "开始时间")
    private Date startTime;

    @Schema(name = "结束时间")
    private Date endTime;

    @Schema(name = "创建时间")
    private Date createTime;

    @Schema(description = "阶段运行列表")
    private List<RunStageVO> stages;
}
