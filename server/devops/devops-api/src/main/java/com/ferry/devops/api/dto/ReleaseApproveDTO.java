package com.ferry.devops.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布审批表单
 */
@Data

public class ReleaseApproveDTO {

    @Schema(description = "发布单id")
    @NotNull(message = "参数不能为空")
    private Long id;

    @Schema(description = "审批意见（至少10字）")
    @NotBlank(message = "参数不能为空")
    private String opinion;

    @Schema(description = "审批节点")
    private Integer node;
}
