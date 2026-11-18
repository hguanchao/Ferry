package com.ferry.devops.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 反馈查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiFeedbackQuery extends Query {

    @Schema(description = "输出id")
    private Long outputId;

    @Schema(description = "是否有帮助")
    private Integer isHelpful;
}
