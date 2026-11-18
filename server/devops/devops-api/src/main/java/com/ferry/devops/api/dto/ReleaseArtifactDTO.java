package com.ferry.devops.api.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布单制品保存表单
 */
@Data

public class ReleaseArtifactDTO {

    @Schema(description = "发布单id")
    @NotNull(message = "参数不能为空")
    private Long rcId;

    @Schema(description = "制品明细列表[{appId,artifactId}]")
    @NotNull(message = "参数不能为空")
    private java.util.List<java.util.Map<String,Object>> items;
}
