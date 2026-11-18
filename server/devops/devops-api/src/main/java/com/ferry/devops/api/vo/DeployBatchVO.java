package com.ferry.devops.api.vo;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部署批次项
 */
@Data
public class DeployBatchVO {

    @Schema(description = "批次id")
    private Long id;

    @Schema(description = "部署id")
    private Long deployId;

    @Schema(description = "批次号")
    private Integer batchNo;

    @Schema(description = "影响实例数")
    private Integer targetCount;

    @Schema(description = "实例清单快照")
    private String targetSnapshot;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date startTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date endTime;

    @Schema(description = "健康结论")
    private Integer healthResult;
}
