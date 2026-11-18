package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 审批记录项
 */
@Data
public class ReleaseApprovalVO {

    @Schema(description = "审批id")
    private Long id;

    @Schema(description = "发布单id")
    private Long rcId;

    @Schema(description = "审批节点")
    private Integer node;

    @Schema(description = "审批人")
    private Long approver;

    @Schema(description = "意见")
    private String opinion;

    @Schema(description = "结果")
    private Integer result;

    @Schema(description = "时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
