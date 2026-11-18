package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 应用负责人
 */
@Data
public class AppOwnerVO {

    @Schema(description = "数据id")
    private Long id;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "1 负责人 2 备份负责人")
    private Integer type;

    @Schema(description = "顺序")
    private Integer sort;
}