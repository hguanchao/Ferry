package com.ferry.devops.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Date;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import lombok.Data;

/**
 * 封网期表单
 */
@Data

public class NetworkClosureDTO {

    @Schema(description = "封网期id")
    private Long id;

    @Schema(description = "开始日期")
    @NotNull(message = "参数不能为空")
    @JsonFormat(pattern = DateUtil.NORM_DATE_PATTERN)
    private Date startDate;

    @Schema(description = "结束日期")
    @NotNull(message = "参数不能为空")
    @JsonFormat(pattern = DateUtil.NORM_DATE_PATTERN)
    private Date endDate;

    @Schema(description = "范围 1全局 2应用 3团队")
    @NotNull(message = "参数不能为空")
    private Integer scope;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "团队id")
    private Long teamId;

    @Schema(description = "原因")
    private String reason;
}
