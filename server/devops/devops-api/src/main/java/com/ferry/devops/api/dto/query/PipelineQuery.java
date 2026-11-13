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

    @Schema(description = "应用id，多选")
    private List<Long> appIds;
}
