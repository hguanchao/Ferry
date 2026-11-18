package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 输出查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiOutputQuery extends Query {

    @Schema(description = "类型 1 归因 2 准入 3 预案 4 异常")
    private Integer type;

    @Schema(description = "业务id")
    private String bizId;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "状态")
    private Integer status;
}
