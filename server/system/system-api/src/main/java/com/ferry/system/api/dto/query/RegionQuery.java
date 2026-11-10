package com.ferry.system.api.dto.query;

import com.ferry.common.jdbc.provider.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegionQuery extends Query {

    @Schema(description = "父级区域id，懒加载时传当前展开行的id；为 0 表示取根节点")
    private Long parentId;

    @Schema(description = "区域名称")
    private String name;

    /**
     * 搜索关键字，同时匹配区域名称与区域编码。
     *
     * 与 parentId 互斥：不传 parentId 时即为跨层级检索，一次返回所有匹配的区划，由前端平铺展示。
     * 区划编码是层级路径码（省 6 位 / 市 6 位 / 区县 6 位 / 街道 12 位），输入编码前缀即可收敛到某一棵子树。
     */
    @Schema(description = "搜索关键字（匹配名称或编码）")
    private String keyword;

    @Schema(description = "状态 0：禁用 1：正常")
    private Integer status;
}
