package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PipelineQuery extends Query {

    @Schema(description = "流水线名称，模糊搜索")
    private String keyword;

    @Schema(description = "流水线id，多选")
    private List<Long> ids;

    @Schema(description = "应用id，多选")
    private List<Long> appIds;

    @Schema(description = "应用负责人id，多选，命中任一即返回")
    private List<Long> ownerIds;

    @Schema(description = "来源模板id，模板派生清单按此过滤")
    private Long sourceTemplateId;
}
