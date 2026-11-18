package com.ferry.devops.api.vo;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 晋级预检结果。
 */
@Data
public class PromotionPrecheckVO {

    @Schema(description = "是否可晋级")
    private Boolean canPromote;

    @Schema(description = "门禁汇总结论")
    private Integer gateConclusion;

    @Schema(description = "是否存在有效豁免")
    private Boolean hasExemption;

    @Schema(description = "来源环境id")
    private Long sourceEnvId;

    @Schema(description = "目标环境id")
    private Long targetEnvId;

    @Schema(description = "形态是否匹配")
    private Boolean formMatched;

    @Schema(description = "校验项")
    private List<Item> items;

    /**
     * 预检项。
     */
    @Data
    public static class Item {

        @Schema(description = "校验项名称")
        private String name;

        @Schema(description = "是否通过")
        private Boolean pass;

        @Schema(description = "说明")
        private String detail;
    }
}
