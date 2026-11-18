package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 度量查询条件(分页)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MetricQuery extends Query {

    @Schema(description = "应用id列表")
    private List<Long> appIds;

    @Schema(description = "开始日期 yyyy-MM-dd")
    private String start;

    @Schema(description = "结束日期 yyyy-MM-dd")
    private String end;
}
