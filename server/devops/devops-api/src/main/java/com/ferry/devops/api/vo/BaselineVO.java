package com.ferry.devops.api.vo;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 存量基线详情。
 */
@Data
public class BaselineVO {

    @Schema(description = "基线id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "扫描工具")
    private String tool;

    @Schema(description = "规则版本")
    private String ruleVersion;

    @Schema(description = "问题总数")
    private Integer totalCount;

    @Schema(description = "各级别数量 JSON")
    private String severityStats;

    @Schema(description = "指纹算法版本")
    private String fingerprintVersion;

    @Schema(description = "是否生效")
    private Integer isActive;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;

    @Schema(description = "新增数")
    private Integer newCount;

    @Schema(description = "已修复数")
    private Integer fixedCount;

    @Schema(description = "遗留数")
    private Integer legacyCount;

    @Schema(description = "待审批的重设申请id列表")
    private List<Long> pendingResetIds;

    @Schema(description = "距上次重设天数")
    private Integer daysSinceReset;
}
