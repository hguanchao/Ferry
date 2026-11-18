package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.ReleaseQuery;
import com.ferry.devops.api.entity.ReleaseCandidate;
import com.ferry.devops.api.vo.ReleaseVO;

/**
 * 发布单列表查询与 VO 转换
 */
public class ReleaseWrapper extends BaseEntityWrapper<ReleaseCandidate, ReleaseVO> {

    public static ReleaseWrapper build() {
        return new ReleaseWrapper();
    }

    @Override
    public ReleaseVO vo(ReleaseCandidate entity) {
        return BeanUtil.copy(entity, ReleaseVO.class);
    }

    /**
     * 发布单查询条件
     */
    public LambdaQueryWrapper<ReleaseCandidate> lambda(ReleaseQuery query) {
        return Wrappers.<ReleaseCandidate>lambdaQuery()
                .like(StrUtil.isNotBlank(query.getRcNo()), ReleaseCandidate::getRcNo, query.getRcNo())
                .like(StrUtil.isNotBlank(query.getTitle()), ReleaseCandidate::getTitle, query.getTitle())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), ReleaseCandidate::getStatus, query.getStatus())
                .eq(ObjUtil.isNotEmpty(query.getRiskLevel()), ReleaseCandidate::getRiskLevel, query.getRiskLevel())
                .eq(ObjUtil.isNotEmpty(query.getCreator()), ReleaseCandidate::getCreator, query.getCreator())
                .orderByDesc(ReleaseCandidate::getCreateTime);
    }
}
