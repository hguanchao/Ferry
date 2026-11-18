package com.ferry.devops.api.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import com.ferry.common.jdbc.provider.Query;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发布单查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReleaseQuery extends Query {

    @Schema(description = "发布单号")
    private String rcNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "风险等级")
    private Integer riskLevel;

    @Schema(description = "创建人")
    private Long creator;

    @Schema(description = "应用id")
    private Long appId;
}
