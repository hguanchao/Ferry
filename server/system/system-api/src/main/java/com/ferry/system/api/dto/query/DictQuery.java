package com.ferry.system.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DictQuery extends Query {

    @Schema(description = "父字典id")
    private Long parentId;

    @Schema(description = "字典码")
    private String code;

    @Schema(description = "字典标签")
    private String label;
}
