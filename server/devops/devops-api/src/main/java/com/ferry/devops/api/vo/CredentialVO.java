package com.ferry.devops.api.vo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 凭据项（密文掩码）
 */
@Data
public class CredentialVO {

    @Schema(description = "凭据id")
    private Long id;

    @Schema(description = "凭据名称")
    private String name;

    @Schema(description = "凭据类型")
    private Integer type;

    @Schema(description = "密文掩码，永远返回 ******")
    private String cipherText;

    @Schema(description = "引用数")
    private Integer refCount;

    @Schema(description = "最近轮换时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private java.util.Date lastRotateTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private java.util.Date createTime;
}