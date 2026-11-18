package com.ferry.devops.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动作目录 VO
 */
@Data
public class ActionCatalogVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "动作标识")
    private String actionId;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "级别 0-3")
    private Integer level;

    @Schema(description = "前置条件")
    private String precondition;

    @Schema(description = "撤销方式")
    private String undoMode;

    @Schema(description = "是否可撤销")
    private Integer isUndoable;

    @Schema(description = "启停")
    private Integer isEnabled;
}
