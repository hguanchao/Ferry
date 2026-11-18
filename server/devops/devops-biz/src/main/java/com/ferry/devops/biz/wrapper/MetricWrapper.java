package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.MetricQuery;
import com.ferry.devops.api.entity.MetricDaily;
import com.ferry.devops.api.vo.MetricVO;

/**
 * 每日指标查询与VO转换
 */
public class MetricWrapper extends BaseEntityWrapper<MetricDaily, MetricVO> {

    public static MetricWrapper build() {
        return new MetricWrapper();
    }

    @Override
    public MetricVO vo(MetricDaily entity) {
        return BeanUtil.copy(entity, MetricVO.class);
    }

    public LambdaQueryWrapper<MetricDaily> lambda(MetricQuery query) {
        return Wrappers.<MetricDaily>lambdaQuery()
                .in(CollUtil.isNotEmpty(query.getAppIds()), MetricDaily::getAppId, query.getAppIds())
                .ge(ObjUtil.isNotEmpty(query.getStart()), MetricDaily::getStatDate, query.getStart())
                .le(ObjUtil.isNotEmpty(query.getEnd()), MetricDaily::getStatDate, query.getEnd())
                .orderByDesc(MetricDaily::getStatDate);
    }
}
