package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用列表查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppQuery extends Query {

    @Schema(description = "应用编码/名称")
    private String code;

    @Schema(description = "应用名称")
    private String name;

    @Schema(description = "所属团队")
    private java.util.List<Long> teamIds;

    @Schema(description = "负责人")
    private java.util.List<Long> ownerIds;

    @Schema(description = "代码平台")
    private java.util.List<Integer> platforms;

    @Schema(description = "核心链路")
    private Integer coreFlag;

    @Schema(description = "最近运行开始")
    private String lastRunStart;

    @Schema(description = "最近运行结束")
    private String lastRunEnd;
}