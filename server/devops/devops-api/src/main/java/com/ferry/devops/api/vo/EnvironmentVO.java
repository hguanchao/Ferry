package com.ferry.devops.api.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 环境列表项
 */
@Data
public class EnvironmentVO {

    @Schema(description = "环境id")
    private Long id;

    @Schema(description = "应用id")
    private Long appId;

    @Schema(description = "环境名")
    private String name;

    @Schema(description = "环境类别 1测试 2预发 3生产")
    private Integer category;

    @Schema(description = "部署类型 1SSH 2Docker 3Kubernetes")
    private Integer deployType;

    @Schema(description = "保护标记")
    private Integer isProtected;

    @Schema(description = "目标资源JSON")
    private String targetConfig;

    @Schema(description = "健康检查配置JSON")
    private String healthConfig;

    @Schema(description = "观察时长分钟")
    private Integer observeMinutes;

    @Schema(description = "配置版本")
    private Integer configVersion;

    @Schema(description = "当前生效制品id")
    private Long currentArtifactId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private java.util.Date createTime;
}