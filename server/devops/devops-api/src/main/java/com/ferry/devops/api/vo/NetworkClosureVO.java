package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 封网期项
 */
@Data
public class NetworkClosureVO {

    @Schema(description = "封网期id")
    private Long id;

    @Schema(description = "开始日期")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date startDate;

    @Schema(description = "结束日期")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date endDate;

    @Schema(description = "范围")
    private Integer scope;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "团队id")
    private Long teamId;

    @Schema(description = "原因")
    private String reason;
}
