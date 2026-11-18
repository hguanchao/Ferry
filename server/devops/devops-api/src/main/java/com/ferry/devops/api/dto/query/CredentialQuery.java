package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 凭据列表查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialQuery extends Query {

    @Schema(description = "凭据名称")
    private String name;

    @Schema(description = "凭据类型")
    private Integer type;
}