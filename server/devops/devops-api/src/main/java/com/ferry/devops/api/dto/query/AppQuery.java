package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 应用列表查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppQuery extends Query {

    @Schema(description = "应用编码/名称，模糊匹配")
    private String code;

    @Schema(description = "应用名称，模糊匹配")
    private String name;

    @Schema(description = "所属团队id集合")
    private List<Long> deptIds;

    @Schema(description = "负责人用户id集合")
    private List<Long> ownerIds;

    @Schema(description = "代码平台集合 1GitLab 2GitHub 3Gitee")
    private List<Integer> repoTypes;

    @Schema(description = "核心链路 0否 1是")
    private Integer coreLink;

    @Schema(description = "关键字")
    private String keyword;
}
