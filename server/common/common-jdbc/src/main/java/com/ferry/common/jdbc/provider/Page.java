package com.ferry.common.jdbc.provider;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.utils.CollUtil;
import lombok.Data;

import java.io.Serializable;

@Data
public class Page implements Serializable {

    /**
     * 转化成mybatis plus中的Page
     *
     * @param query 查询条件
     * @return IPage
     */
    public static <T> IPage<T> create(Query query) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page
                = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.getCurrent(), query.getSize());
        if (CollUtil.isNotEmpty(query.getOrders())){
            page.addOrder(query.getOrders());
        }
        return page;
    }
}
