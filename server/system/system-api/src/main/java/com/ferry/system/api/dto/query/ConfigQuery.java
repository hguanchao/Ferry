package com.ferry.system.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigQuery extends Query {

    @Schema(description = "参数名称")
    private String name;

    @Schema(description = "参数键名")
    private String code;

    @Schema(description = "是否系统内置 0：否 1：是")
    private Integer isInternal;
}
