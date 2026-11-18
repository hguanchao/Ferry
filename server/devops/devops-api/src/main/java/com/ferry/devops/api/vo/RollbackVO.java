package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 回滚记录列表项
 */
@Data
public class RollbackVO {

    @Schema(description = "回滚id")
    private Long id;

    @Schema(description = "关联部署")
    private Long deployId;

    @Schema(description = "环境id")
    private Long envId;

    @Schema(description = "环境名称")
    private String envName;

    @Schema(description = "回滚目标制品")
    private Long artifactId;

    @Schema(description = "制品版本")
    private String artifactVersion;

    @Schema(description = "回滚前制品")
    private Long currentArtifactId;

    @Schema(description = "原因")
    private String reason;

    @Schema(description = "操作人")
    private Long operator;

    @Schema(description = "确认人")
    private Long confirmer;

    @Schema(description = "结果 1成功 2失败")
    private Integer result;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date startTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date endTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
