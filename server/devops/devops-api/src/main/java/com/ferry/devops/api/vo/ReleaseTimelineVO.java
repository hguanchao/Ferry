package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布单时间线项
 */
@Data
public class ReleaseTimelineVO {

    @Schema(description = "时间线id")
    private Long id;

    @Schema(description = "发布单id")
    private Long rcId;

    @Schema(description = "节点")
    private String node;

    @Schema(description = "操作人")
    private Long operator;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
