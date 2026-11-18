package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 制品晋级表单。只写环境标记，不改制品本身。
 */
@Data
public class ArtifactPromoteDTO {

    @Schema(description = "制品id")
    @NotNull(message = "制品id不能为空")
    private Long id;

    @Schema(description = "目标环境id")
    @NotNull(message = "目标环境id不能为空")
    private Long targetEnvId;

    @Schema(description = "备注，晋级生产时必填")
    private String note;
}
