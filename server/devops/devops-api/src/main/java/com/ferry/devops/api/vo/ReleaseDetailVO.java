package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import java.util.List;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布单详情
 */
@Data
public class ReleaseDetailVO {

    @Schema(description = "发布单id")
    private Long id;

    @Schema(description = "发布单号")
    private String rcNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "风险等级")
    private Integer riskLevel;

    @Schema(description = "加权分")
    private java.math.BigDecimal riskScore;

    @Schema(description = "五因子明细JSON")
    private String riskDetail;

    @Schema(description = "三层影响面")
    private String impactJson;

    @Schema(description = "计划窗口开始")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date windowStart;

    @Schema(description = "计划窗口结束")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date windowEnd;

    @Schema(description = "变更单id")
    private Long changeTicketId;

    @Schema(description = "创建人")
    private Long creator;

    @Schema(description = "制品列表")
    private java.util.List<ReleaseArtifactVO> artifacts;

    @Schema(description = "检查项列表")
    private java.util.List<ReleaseCheckVO> checks;

    @Schema(description = "时间线")
    private java.util.List<ReleaseTimelineVO> timeline;
}
