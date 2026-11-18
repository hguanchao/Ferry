package com.ferry.devops.api.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 技术债/存量问题看板项。
 */
@Data
public class TechDebtVO {

    @Schema(description = "问题id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "指纹")
    private String fingerprint;

    @Schema(description = "级别 1阻断 2高危 3中危 4低危")
    private Integer severity;

    @Schema(description = "规则类型")
    private Integer ruleType;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "状态 1新增 2遗留 3已修复 4误报")
    private Integer status;

    @Schema(description = "修复责任人")
    private Long assignee;

    @Schema(description = "首次出现")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date firstSeen;

    @Schema(description = "最近出现")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date lastSeen;
}
