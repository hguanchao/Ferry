package com.ferry.devops.api.dto;

import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.devops.api.enums.PipelineStageTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 流水线阶段
 */
@Data
public class PipelineStageDTO {

    @Schema(description = "阶段类型")
    @NotBlank(message = "阶段类型不能为空")
    private String type;

    @Schema(description = "阶段名称")
    @NotBlank(message = "阶段名称不能为空")
    @Size(max = 50, message = "阶段名称不能超过 50 个字符")
    private String name;

    @Schema(description = "超时时间（分钟）")
    @NotNull(message = "阶段超时时间不能为空")
    @Min(value = 1, message = "阶段超时时间不能小于 1 分钟")
    @Max(value = 1440, message = "阶段超时时间不能超过 1440 分钟")
    private Integer timeoutMinutes;

    @Schema(description = "失败后是否继续")
    @NotNull(message = "请设置阶段失败后是否继续")
    private Boolean continueOnFailure;

    @Schema(description = "产出物声明")
    private List<@Size(max = 200, message = "产出物声明不能超过 200 个字符") String> artifacts;

    @Schema(description = "任务列表")
    @Valid
    private List<PipelineTaskDTO> tasks;
}
