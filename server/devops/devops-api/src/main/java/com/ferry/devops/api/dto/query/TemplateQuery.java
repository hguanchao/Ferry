package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 模板列表查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateQuery extends Query {

    @Schema(description = "1 组织级 2 团队级")
    private Integer scope;

    @Schema(description = "团队id")
    private Long teamId;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "启停")
    private Integer isEnabled;

    @Schema(description = "模板id集合")
    private List<Long> ids;
}
