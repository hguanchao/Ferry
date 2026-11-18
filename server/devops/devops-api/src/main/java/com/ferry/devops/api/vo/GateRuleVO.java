package com.ferry.devops.api.vo;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 门禁规则。
 */
@Data
public class GateRuleVO {

    @Schema(description = "规则id")
    private Long id;

    @Schema(description = "作用域 1 组织 2 应用 3 分支")
    private Integer scopeType;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "分支通配")
    private String branchPattern;

    @Schema(description = "规则类型")
    private Integer ruleType;

    @Schema(description = "规则名")
    private String ruleName;

    @Schema(description = "阈值")
    private String threshold;

    @Schema(description = "比较方向 1 不低于 2 不高于")
    private Integer compareDirection;

    @Schema(description = "作用阶段")
    private String stageTypes;

    @Schema(description = "是否允许豁免")
    private Integer isExemptAllowed;

    @Schema(description = "启停")
    private Integer isEnabled;

    @Schema(description = "规则版本")
    private Integer version;

    @Schema(description = "最后修改人")
    private Long updateUser;

    @Schema(description = "修改时间")
    private java.util.Date updateTime;
}
