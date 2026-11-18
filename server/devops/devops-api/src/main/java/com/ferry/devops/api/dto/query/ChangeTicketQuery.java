package com.ferry.devops.api.dto.query;

import io.swagger.v3.oas.annotations.media.Schema;
import com.ferry.common.jdbc.provider.Query;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 变更单查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChangeTicketQuery extends Query {

    @Schema(description = "变更单号")
    private String ticketNo;

    @Schema(description = "类型")
    private Integer type;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "申请人")
    private Long applicant;

    @Schema(description = "标题关键字")
    private String keyword;

    @Schema(description = "窗口起")
    private String windowStart;

    @Schema(description = "窗口止")
    private String windowEnd;
}
