package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流水线模板查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PipelineTemplateQuery extends Query {

    @Schema(description = "模板id，派生流水线清单按此过滤")
    private Long id;

    @Schema(description = "模板层级 0：组织级 1：团队级")
    private Integer scope;

    @Schema(description = "模板名称，模糊搜索")
    private String keyword;

    @Schema(description = "状态 0：停用 1：启用")
    private Integer status;
}
