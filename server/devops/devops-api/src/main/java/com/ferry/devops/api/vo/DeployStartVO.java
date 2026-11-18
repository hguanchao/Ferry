package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部署启动结果
 */
@Data
public class DeployStartVO {

    @Schema(description = "部署id")
    private Long id;

    @Schema(description = "部署编号")
    private String deployNo;
}
