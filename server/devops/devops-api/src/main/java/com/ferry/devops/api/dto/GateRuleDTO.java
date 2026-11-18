package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 门禁规则表单。
 */
@Data
public class GateRuleDTO {

    @Schema(description = "规则id")
    private Long id;

    @Schema(description = "作用域 1 组织 2 应用 3 分支")
    @NotNull(message = "作用域不能为空")
    private Integer scopeType;

    @Schema(description = "应用id，应用/分支级必填")
    private Long appId;

    @Schema(description = "分支通配，仅分支级")
    private String branchPattern;

    @Schema(description = "规则类型 1 单测通过率 2 行覆盖率 3 分支覆盖率 4 阻断漏洞 5 高危依赖 6 许可证黑名单")
    @NotNull(message = "规则类型不能为空")
    private Integer ruleType;

    @Schema(description = "规则名")
    @NotBlank(message = "规则名不能为空")
    private String ruleName;

    @Schema(description = "阈值")
    @NotBlank(message = "阈值不能为空")
    private String threshold;

    @Schema(description = "比较方向 1 不低于 2 不高于")
    @NotNull(message = "比较方向不能为空")
    private Integer compareDirection;

    @Schema(description = "作用阶段，逗号分隔")
    @NotBlank(message = "作用阶段不能为空")
    private String stageTypes;

    @Schema(description = "是否允许豁免")
    private Integer isExemptAllowed;

    @Schema(description = "启停")
    private Integer isEnabled;
}
