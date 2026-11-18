package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 回滚候选制品
 */
@Data
public class RollbackCandidateVO {

    @Schema(description = "制品id")
    private Long artifactId;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "摘要")
    private String digest;

    @Schema(description = "发布时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date deployedAt;

    @Schema(description = "使用状态 1当前 2历史")
    private Integer prodUseStatus;

    @Schema(description = "标注")
    private String label;
}
