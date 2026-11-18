package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知渠道查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifyChannelQuery extends Query {

    @Schema(description = "渠道类型")
    private Integer type;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "渠道名")
    private String name;
}
