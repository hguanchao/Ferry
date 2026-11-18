package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 准入检查项
 */
@Data
public class ReleaseCheckVO {

    @Schema(description = "检查项id")
    private Long id;

    @Schema(description = "发布单id")
    private Long rcId;

    @Schema(description = "检查项类型")
    private Integer checkType;

    @Schema(description = "检查项名称")
    private String checkName;

    @Schema(description = "系统结论")
    private Integer systemConclusion;

    @Schema(description = "人工结论")
    private Integer manualConclusion;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "证据")
    private String evidence;
}
