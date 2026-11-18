package com.ferry.devops.api.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 制品清理任务。
 */
@Data
public class CleanTaskVO {

    @Schema(description = "任务id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "任务时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date taskTime;

    @Schema(description = "待清理数")
    private Integer pendingCount;

    @Schema(description = "实际清理数")
    private Integer deletedCount;

    @Schema(description = "1 待确认 2 已执行 3 失败")
    private Integer status;

    @Schema(description = "清单文件地址")
    private String manifestFile;

    @Schema(description = "执行人")
    private Long operator;
}
