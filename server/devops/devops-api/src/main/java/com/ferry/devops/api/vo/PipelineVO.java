package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 流水线列表项
 */
@Data
public class PipelineVO {

    @Schema(description = "流水线id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "流水线名称")
    private String name;

    @Schema(description = "状态 0停用 1启用")
    private Integer status;

    @Schema(description = "当前版本号")
    private Integer version;

    @Schema(description = "创建时间")
    private Date createTime;
}
