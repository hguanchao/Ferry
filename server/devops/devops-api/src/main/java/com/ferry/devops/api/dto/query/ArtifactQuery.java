package com.ferry.devops.api.dto.query;

import java.util.Date;
import java.util.List;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 制品列表查询条件。8 项筛选 + 包含已删除开关。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArtifactQuery extends Query {

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "制品形态列表")
    private List<Integer> forms;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "分支")
    private String branch;

    @Schema(description = "环境id")
    private Long envId;

    @Schema(description = "是否被生产使用")
    private Integer prodUseStatus;

    @Schema(description = "构建时间开始")
    private Date buildStart;

    @Schema(description = "构建时间结束")
    private Date buildEnd;

    @Schema(description = "构建人")
    private Long builder;

    @Schema(description = "是否包含已删除制品")
    private Boolean includeDeleted;
}
