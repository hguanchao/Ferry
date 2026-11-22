package com.ferry.devops.api.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 运行阶段门禁结果。
 */
@Data
public class RunStageGateVO {

    @Schema(description = "结果id")
    private Long id;

    @Schema(description = "运行id")
    private Long runId;

    @Schema(description = "运行阶段id")
    private Long runStageId;

    @Schema(description = "规则id")
    private Long ruleId;

    @Schema(description = "规则类型 1=单元测试通过率 2=行覆盖率 3=分支覆盖率 4=阻断级漏洞数 5=高危依赖漏洞数 6=许可证黑名单")
    private Integer ruleType;

    @Schema(description = "规则版本号")
    private Integer ruleVersion;

    @Schema(description = "规则快照(JSON)")
    private String ruleSnapshot;

    @Schema(description = "当前值")
    private String currentValue;

    @Schema(description = "阈值")
    private String threshold;

    @Schema(description = "状态 1=通过 2=不通过 3=不适用 4=已豁免 5=数据获取失败")
    private Integer status;

    @Schema(description = "证据链接")
    private String evidence;

    @Schema(description = "报告来源")
    private String source;

    @Schema(description = "报告schema版本")
    private String schemaVersion;

    @Schema(description = "原因说明 数据获取失败/不适用/豁免原因")
    private String fetchError;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date updateTime;
}
