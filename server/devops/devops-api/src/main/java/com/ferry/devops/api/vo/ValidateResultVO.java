package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 定义校验结果
 */
@Data
public class ValidateResultVO {

    @Schema(description = "是否合法")
    private Boolean valid;

    @Schema(description = "错误列表（带行号）")
    private List<ValidateError> errors;

    /**
     * 单条校验错误
     */
    @Data
    public static class ValidateError {

        @Schema(description = "错误行号，0 表示整体错误")
        private Integer line;

        @Schema(description = "错误说明")
        private String message;
    }
}
