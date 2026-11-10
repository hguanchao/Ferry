package com.ferry.system.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.system.api.dto.query.RegionQuery;
import com.ferry.system.api.entity.Region;
import com.ferry.system.api.vo.tree.RegionTree;

public class RegionWrapper extends BaseEntityWrapper<Region, RegionTree> {
    public static RegionWrapper build() {
        return new RegionWrapper();
    }

    @Override
    public RegionTree vo(Region entity) {
        return BeanUtil.copy(entity, RegionTree.class);
    }

    public LambdaQueryWrapper<Region> lambda(RegionQuery query) {
        Long parentId = query.getParentId();
        String name = query.getName();
        Integer status = query.getStatus();
        return Wrappers.<Region>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(parentId), Region::getParentId, parentId)
                .like(StrUtil.isNotBlank(name), Region::getName, name)
                .eq(ObjUtil.isNotEmpty(status), Region::getStatus, status)
                .orderByAsc(Region::getSort).orderByDesc(Region::getCreateTime);
    }
}
