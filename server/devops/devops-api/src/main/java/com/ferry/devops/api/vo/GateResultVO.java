package com.ferry.devops.api.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 门禁结果。
 */
@Data
public class GateResultVO {

    @Schema(description = "结果id")
    private Long id;

    @Schema(description = "运行id")
    private Long runId;

    @Schema(description = "阶段id")
    private Long stageId;

    @Schema(description = "规则id")
    private Long ruleId;

    @Schema(description = "规则快照")
    private String ruleSnapshot;

    @Schema(description = "当前值")
    private String currentValue;

    @Schema(description = "阈值")
    private String threshold;

    @Schema(description = "结论 1 通过 2 不通过 3 不适用 4 已豁免 5 数据获取失败")
    private Integer conclusion;

    @Schema(description = "证据链接")
    private String evidence;

    @Schema(description = "报告来源")
    private String source;

    @Schema(description = "schema 版本")
    private String schemaVersion;

    @Schema(description = "获取失败原因")
    private String fetchError;

    @Schema(description = "判定时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
