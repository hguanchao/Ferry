package com.ferry.devops.api.vo;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 影响面结果
 */
@Data
public class ImpactVO {

    @Schema(description = "三层影响面")
    private java.util.List<java.util.Map<String,Object>> layers;
}
