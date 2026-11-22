package com.ferry.devops.api.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 门禁豁免。
 */
@Data
public class GateExemptionVO {

    @Schema(description = "豁免id")
    private Long id;

    @Schema(description = "运行阶段门禁结果id")
    private Long runStageGateId;

    @Schema(description = "运行id")
    private Long runId;

    @Schema(description = "规则id")
    private Long ruleId;

    @Schema(description = "理由")
    private String reason;

    @Schema(description = "到期时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date expireAt;

    @Schema(description = "关联问题单")
    private String issueNo;

    @Schema(description = "申请人")
    private Long applicant;

    @Schema(description = "1 生效 2 已到期 3 已撤销")
    private Integer status;

    @Schema(description = "申请时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
