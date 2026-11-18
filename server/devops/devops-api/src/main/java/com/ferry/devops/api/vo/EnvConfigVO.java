package com.ferry.devops.api.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 环境配置项
 */
@Data
public class EnvConfigVO {

    @Schema(description = "配置id")
    private Long id;

    @Schema(description = "环境id")
    private Long envId;

    @Schema(description = "配置键")
    private String configKey;

    @Schema(description = "配置值，凭据类返回掩码")
    private String configValue;

    @Schema(description = "值类型 1普通 2凭据")
    private Integer valueType;

    @Schema(description = "凭据id")
    private Long credentialId;

    @Schema(description = "配置版本")
    private Integer version;

    @Schema(description = "是否加密")
    private Integer isEncrypted;

    @Schema(description = "修改时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private java.util.Date updateTime;
}