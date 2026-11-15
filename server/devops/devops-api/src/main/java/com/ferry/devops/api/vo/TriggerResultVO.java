package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TriggerResultVO {

    @Schema(description = "触发结果 CREATED/MERGED/REUSED/REJECTED/PENDING_REUSE")
    private String result;

    @Schema(description = "触发结果名称")
    private String resultName;

    @Schema(description = "本次关联的运行id，PENDING_REUSE 时为命中成功的既有运行id")
    private Long runId;

    @Schema(description = "命中成功的既有运行id，等待用户确认复用")
    private Long reuseCandidateRunId;

    @Schema(description = "拒绝原因，仅已拒绝时返回")
    private String rejectReason;
}
