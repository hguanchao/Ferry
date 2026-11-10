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
public class DictTree extends TreeNode {

    @Schema(description = "租户编码")
    private String tenantId;

    @Schema(description = "字典码")
    private String code;

    @Schema(description = "字典标签")
    private String label;

    @Schema(description = "字典值")
    private String value;

    @Schema(description = "显示排序")
    private Integer sort;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = DateUtil.NORM_DATETIME_PATTERN)
    private Date createTime;
}
