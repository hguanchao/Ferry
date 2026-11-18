package com.ferry.devops.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import lombok.Data;

/**
 * 变更单表单
 */
@Data

public class ChangeTicketDTO {

    @Schema(description = "变更单id")
    private Long id;

    @Schema(description = "类型 1常规 2紧急 3标准")
    @NotNull(message = "参数不能为空")
    private Integer type;

    @Schema(description = "标题")
    @NotBlank(message = "参数不能为空")
    private String title;

    @Schema(description = "变更内容")
    private String content;

    @Schema(description = "计划窗口开始")
    @NotNull(message = "参数不能为空")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date windowStart;

    @Schema(description = "计划窗口结束")
    @NotNull(message = "参数不能为空")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date windowEnd;

    @Schema(description = "回滚方案")
    private String rollbackPlan;

    @Schema(description = "影响评估")
    private String impactJson;

    @Schema(description = "关联应用id，逗号分隔")
    private String appIds;
}
