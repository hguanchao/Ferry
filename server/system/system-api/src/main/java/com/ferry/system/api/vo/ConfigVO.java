package com.ferry.system.api.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class ConfigVO {

    @Schema(description = "参数id")
    private Long id;

    @Schema(description = "参数名称")
    private String name;

    @Schema(description = "参数键名")
    private String code;

    @Schema(description = "参数键值")
    private String value;

    @Schema(description = "是否系统内置 0：否 1：是")
    private Integer isInternal;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
