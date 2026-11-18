package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 技术债/存量问题查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TechDebtQuery extends Query {

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "严重级别 1阻断 2高危 3中危 4低危")
    private Integer severity;

    @Schema(description = "规则类型")
    private Integer ruleType;

    @Schema(description = "状态 1新增 2遗留 3已修复 4误报")
    private Integer status;

    @Schema(description = "指纹")
    private String fingerprint;

    @Schema(description = "首次出现开始")
    private String firstSeenStart;

    @Schema(description = "首次出现结束")
    private String firstSeenEnd;
}
