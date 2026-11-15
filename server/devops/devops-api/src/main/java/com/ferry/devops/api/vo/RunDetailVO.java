package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class RunDetailVO extends RunVO {

    @Schema(description = "五项预校验结论")
    private List<PrecheckItemVO> prechecks;

    @Schema(description = "阶段列表，按执行顺序排列")
    private List<RunStageVO> stages;
}
