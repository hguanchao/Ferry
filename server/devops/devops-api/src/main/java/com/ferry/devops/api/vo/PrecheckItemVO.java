package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单项预校验结论
 */
@Data
public class PrecheckItemVO {

    @Schema(description = "1仓库信息 2分支 3提交格式 4定义匹配 5幂等检查")
    private Integer type;

    @Schema(description = "1通过 2不通过 3合并 4复用")
    private Integer conclusion;

    @Schema(description = "明细")
    private String detail;
}
