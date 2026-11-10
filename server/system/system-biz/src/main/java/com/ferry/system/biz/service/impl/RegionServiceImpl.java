package com.ferry.system.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.TreeUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.system.api.dto.RegionDTO;
import com.ferry.system.api.dto.query.RegionQuery;
import com.ferry.system.api.entity.Region;
import com.ferry.system.api.vo.tree.RegionTree;
import com.ferry.system.biz.mapper.RegionMapper;
import com.ferry.system.biz.service.IRegionService;
import com.ferry.system.biz.wrapper.RegionWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements IRegionService {

    @Override
    public List<RegionTree> treeList(RegionQuery body) {
        List<Region> list = list(RegionWrapper.build().lambda(body));
        List<RegionTree> regionList = BeanUtil.copy(list, RegionTree.class);
        List<RegionTree> treeList = TreeUtil.build(regionList);
        return CollUtil.isNotEmpty(treeList) ? treeList : regionList;
    }

    @Override
    public List<RegionTree> lazyList(RegionQuery body) {
        return baseMapper.selectLazyList(body);
    }

    @Override
    public RegionTree detail(Long id) {
        Region region = getById(id);
        if (ObjUtil.isEmpty(region)) {
            throw new ServiceException("区域不存在");
        }
        return BeanUtil.copy(region, RegionTree.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(RegionDTO body) {
        Region region = BeanUtil.copy(body, Region.class);
        if (!TreeUtil.isRoot(region.getParentId())) {
            if (ObjUtil.isEmpty(getById(region.getParentId()))) {
                throw new ServiceException("上级区域不存在");
            }
        }
        return saveOrUpdate(region);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的区域");
        }
        boolean isSuccess = removeByIds(ids);
        if (isSuccess) {
            remove(Wrappers.<Region>lambdaQuery().in(Region::getParentId, ids));
        }
        return isSuccess;
    }

}
