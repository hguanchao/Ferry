package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 变更单列表项
 */
@Data
public class ChangeTicketVO {

    @Schema(description = "变更单id")
    private Long id;

    @Schema(description = "变更单号")
    private String ticketNo;

    @Schema(description = "类型")
    private Integer type;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "计划窗口开始")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date windowStart;

    @Schema(description = "计划窗口结束")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date windowEnd;

    @Schema(description = "申请人")
    private Long applicant;

    @Schema(description = "审批人")
    private Long approver;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
