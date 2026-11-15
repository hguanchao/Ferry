package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PrecheckItemVO {

    @Schema(description = "预校验项 REPO/BRANCH/COMMIT/DEFINITION/IDEMPOTENT")
    private String item;

    @Schema(description = "预校验项名称")
    private String itemName;

    @Schema(description = "结论 PASS/FAIL/NA")
    private String conclusion;

    @Schema(description = "结论说明")
    private String detail;
}
