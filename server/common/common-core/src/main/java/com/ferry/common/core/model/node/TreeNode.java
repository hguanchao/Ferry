package com.ferry.common.core.model.node;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 树节点基类，配合 TreeUtil 组树。
 *
 * <p>id 与 parentId 序列化为字符串，前端按 string 处理避免 Long 精度丢失；
 * children 为空时不输出，前端据此判定叶子节点。
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
