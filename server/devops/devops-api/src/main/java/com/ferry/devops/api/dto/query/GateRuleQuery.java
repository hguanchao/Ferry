package com.ferry.devops.api.dto.query;

import java.util.List;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 门禁规则查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GateRuleQuery extends Query {

    @Schema(description = "作用域 1 组织 2 应用 3 分支")
    private Integer scopeType;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "规则类型")
    private Integer ruleType;

    @Schema(description = "规则名")
    private String ruleName;

    @Schema(description = "启停")
    private Integer isEnabled;

    @Schema(description = "规则类型列表")
    private List<Integer> ruleTypes;
}
