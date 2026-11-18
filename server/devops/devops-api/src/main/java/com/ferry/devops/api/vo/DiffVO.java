package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 两版本行级差异
 */
@Data
public class DiffVO {

    @Schema(description = "左侧版本内容")
    private String left;

    @Schema(description = "右侧版本内容")
    private String right;

    @Schema(description = "差异行")
    private List<DiffLine> lines;

    /**
     * 单行差异
     */
    @Data
    public static class DiffLine {

        @Schema(description = "added/removed/unchanged")
        private String type;

        @Schema(description = "左侧行号")
        private Integer leftNo;

        @Schema(description = "右侧行号")
        private Integer rightNo;

        @Schema(description = "行文本")
        private String text;
    }
}
