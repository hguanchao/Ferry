package com.ferry.devops.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 技术债指派表单。
 */
@Data
public class TechDebtAssignDTO {

    @Schema(description = "问题id")
    @NotNull(message = "问题id不能为空")
    private Long id;

    @Schema(description = "修复责任人")
    @NotNull(message = "责任人不能为空")
    private Long assigneeId;
}
