package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 健康检查采样项
 */
@Data
public class HealthSampleVO {

    @Schema(description = "采样id")
    private Long id;

    @Schema(description = "部署id")
    private Long deployId;

    @Schema(description = "批次id")
    private Long batchId;

    @Schema(description = "检查项")
    private Integer checkType;

    @Schema(description = "采样值")
    private String value;

    @Schema(description = "阈值")
    private String threshold;

    @Schema(description = "结论")
    private Integer conclusion;

    @Schema(description = "采样时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date sampledAt;
}
