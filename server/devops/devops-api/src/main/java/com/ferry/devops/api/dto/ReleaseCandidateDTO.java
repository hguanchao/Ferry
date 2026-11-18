package com.ferry.devops.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import lombok.Data;

/**
 * 发布单表单
 */
@Data

public class ReleaseCandidateDTO {

    @Schema(description = "发布单id")
    private Long id;

    @Schema(description = "标题")
    @NotBlank(message = "参数不能为空")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "计划窗口开始")
    @NotNull(message = "参数不能为空")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date windowStart;

    @Schema(description = "计划窗口结束")
    @NotNull(message = "参数不能为空")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date windowEnd;

    @Schema(description = "变更单id")
    private Long changeTicketId;
}
