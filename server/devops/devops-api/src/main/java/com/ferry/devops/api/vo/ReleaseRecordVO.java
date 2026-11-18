package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布记录项
 */
@Data
public class ReleaseRecordVO {

    @Schema(description = "发布记录id")
    private Long id;

    @Schema(description = "发布单id")
    private Long rcId;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "环境id")
    private Long envId;

    @Schema(description = "环境名称")
    private String envName;

    @Schema(description = "部署类型")
    private Integer deployType;

    @Schema(description = "制品id")
    private Long artifactId;

    @Schema(description = "结果")
    private Integer result;

    @Schema(description = "操作人")
    private Long operator;

    @Schema(description = "确认人")
    private Long confirmer;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date startTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date endTime;

    @Schema(description = "是否回滚过")
    private Integer isRolledBack;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
