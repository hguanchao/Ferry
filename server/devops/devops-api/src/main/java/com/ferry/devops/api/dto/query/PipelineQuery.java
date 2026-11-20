package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 流水线列表查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PipelineQuery extends Query {

    @Schema(description = "应用id")
    private List<Long> appIds;

    @Schema(description = "流水线名称")
    private String name;

    @Schema(description = "状态 0停用 1启用")
    private Integer status;

    @Schema(description = "运行状态")
    private List<Integer> statuses;

    @Schema(description = "分支")
    private List<String> branches;

    @Schema(description = "触发方式")
    private List<Integer> triggerTypes;

    @Schema(description = "门禁结论")
    private List<Integer> gateConclusions;

    @Schema(description = "是否存在豁免 0否 1是")
    private Integer hasExemption;

    @Schema(description = "制品形态")
    private List<Integer> artifactForms;

    @Schema(description = "负责人")
    private List<Long> ownerIds;

    @Schema(description = "最近运行开始")
    private String lastRunStart;

    @Schema(description = "最近运行结束")
    private String lastRunEnd;

    @Schema(description = "指标卡过滤：运行中")
    private Integer metricRunning;

    @Schema(description = "指标卡过滤：今日失败")
    private Integer metricFailedToday;

    @Schema(description = "指标卡过滤：门禁阻塞")
    private Integer metricGateBlocked;

    @Schema(description = "指标卡过滤：待准入")
    private Integer metricPendingAdmission;

    @Schema(description = "指标卡过滤：发布观察中")
    private Integer metricObserving;
}
