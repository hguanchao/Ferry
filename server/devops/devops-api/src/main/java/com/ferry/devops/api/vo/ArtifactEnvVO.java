package com.ferry.devops.api.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 制品环境标记。
 */
@Data
public class ArtifactEnvVO {

    @Schema(description = "记录id")
    private Long id;

    @Schema(description = "环境id")
    private Long envId;

    @Schema(description = "环境类别快照")
    private Integer envCategory;

    @Schema(description = "晋级来源环境")
    private Long sourceEnvId;

    @Schema(description = "晋级人")
    private Long promoter;

    @Schema(description = "晋级时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date promotedAt;
}
