package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppQuery extends Query {

    @Schema(description = "应用编码或名称，模糊搜索")
    private String keyword;

    @Schema(description = "所属团队id，多选")
    private List<Long> deptIds;

    @Schema(description = "应用负责人id，多选")
    private List<Long> ownerIds;

    @Schema(description = "代码托管平台，多选")
    private List<String> repoTypes;

    @Schema(description = "核心链路 0：否 1：是")
    private Integer isCore;

    @Schema(description = "状态 0：停用 1：正常")
    private Integer status;
}
