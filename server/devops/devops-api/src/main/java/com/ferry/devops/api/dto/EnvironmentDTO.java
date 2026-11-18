package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 环境建档/编辑表单
 */
@Data
public class EnvironmentDTO {

    @Schema(description = "环境id")
    private Long id;

    @Schema(description = "应用id")
    @NotNull(message = "应用id不能为空")
    private Long appId;

    @Schema(description = "环境名称，同一应用内唯一")
    @NotBlank(message = "环境名称不能为空")
    private String name;

    @Schema(description = "环境类别 1测试 2预发 3生产")
    @NotNull(message = "环境类别不能为空")
    private Integer category;

    @Schema(description = "部署类型 1SSH 2Docker 3Kubernetes")
    @NotNull(message = "部署类型不能为空")
    private Integer deployType;

    @Schema(description = "目标资源JSON")
    private String targetConfig;

    @Schema(description = "健康检查配置JSON")
    private String healthConfig;

    @Schema(description = "观察时长分钟")
    private Integer observeMinutes;

    @Schema(description = "保护标记，生产自动置1")
    private Integer isProtected;

    @Schema(description = "生产类别二次确认标记")
    private Boolean prodConfirm;
}