package com.ferry.system.api.vo.tree;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ferry.common.core.model.node.TreeNode;
import com.ferry.common.core.utils.DateUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegionTree extends TreeNode {

    @Schema(description = "区域编码")
    private String code;

    @Schema(description = "区域名称")
    private String name;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态 0：禁用 1：正常")
    private Integer status;

    @Schema(description = "是否叶子节点（无下级区划）")
    private Boolean isLeaf;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
