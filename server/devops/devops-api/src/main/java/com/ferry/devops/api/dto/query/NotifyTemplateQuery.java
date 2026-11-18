package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知模板查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifyTemplateQuery extends Query {

    @Schema(description = "事件")
    private String event;

    @Schema(description = "渠道类型")
    private Integer channelType;

    @Schema(description = "启停")
    private Integer isEnabled;
}
