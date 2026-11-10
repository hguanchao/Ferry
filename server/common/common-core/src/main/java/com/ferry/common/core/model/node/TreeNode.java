package com.ferry.common.core.model.node;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * TreeNode
 */
@Data
public class TreeNode {

    @Schema(description = "节点id")
    @JsonSerialize(using = ToStringSerializer.class)
    protected Long id;

    @Schema(description = "父节点id")
    @JsonSerialize(using = ToStringSerializer.class)
    protected Long parentId;

    @Schema(description = "子节点")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected List<? extends TreeNode> children = new ArrayList<>();
}
