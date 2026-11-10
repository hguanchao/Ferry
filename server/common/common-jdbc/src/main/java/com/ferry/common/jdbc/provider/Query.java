package com.ferry.common.jdbc.provider;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Tag(name = "查询条件")
public class Query implements Serializable {

    /**
     * 当前页
     */
    @Schema(description = "当前页", defaultValue = "1")
    private Integer current = 1;

    /**
     * 每页的数量
     */
    @Schema(description = "每页的数量", defaultValue = "10")
    private Integer size = 10;

    /**
     * 排序的字段
     */
    @Schema(description = "排序的字段")
    List<OrderItem> orders;
}
