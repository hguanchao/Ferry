package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布单列表项
 */
@Data
public class ReleaseVO {

    @Schema(description = "发布单id")
    private Long id;

    @Schema(description = "发布单号")
    private String rcNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "风险等级")
    private Integer riskLevel;

    @Schema(description = "加权分")
    private java.math.BigDecimal riskScore;

    @Schema(description = "计划窗口开始")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date windowStart;

    @Schema(description = "计划窗口结束")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date windowEnd;

    @Schema(description = "创建人")
    private Long creator;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
