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

    @Schema(description = "运行状态过滤（1排队 2运行中 3阻塞 4成功 5失败 6超时 7已取消 8结果待确认）：存在指定状态的运行")
    private List<Integer> statuses;

    @Schema(description = "指标卡过滤：运行中（存在排队/运行中的运行）")
    private Integer metricRunning;

    @Schema(description = "指标卡过滤：今日失败（当日存在失败的运行）")
    private Integer metricFailedToday;

    @Schema(description = "指标卡过滤：门禁阻塞（存在被门禁阻塞的运行）")
    private Integer metricGateBlocked;

    @Schema(description = "指标卡过滤：待准入（存在卡在准入阶段的运行）")
    private Integer metricPendingAdmission;

    @Schema(description = "指标卡过滤：发布观察中（存在结果待确认的运行）")
    private Integer metricObserving;
}
