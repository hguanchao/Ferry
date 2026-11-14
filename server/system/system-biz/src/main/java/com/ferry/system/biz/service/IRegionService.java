package com.ferry.system.biz.service;

import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.RegionDTO;
import com.ferry.system.api.dto.query.RegionQuery;
import com.ferry.system.api.entity.Region;
import com.ferry.system.api.vo.tree.RegionTree;

import java.util.List;

public interface IRegionService extends IService<Region> {

    /**
     * 区域管理-树结构列表
     *
     * @param body 查询条件
     * @return 树结构列表
     */
    List<RegionTree> treeList(RegionQuery body);

    /**
     * 区域管理-懒加载列表
     *
     * @param body 查询条件
     * @return 列表数据
     */
    List<RegionTree> lazyList(RegionQuery body);

    /**
     * 区域管理-详情信息
     *
     * @param id 区域id
     * @return 区域详情
     */
    RegionTree detail(Long id);

    /**
     * 区域管理-编辑信息
     *
     * @param body 表单数据
     * @return 是否成功
     */
    boolean save(RegionDTO body);

    /**
     * 区域管理-删除信息
     *
     * @param ids 区域id，支持批量
     * @return 是否成功
     */
    boolean deleted(List<Long> ids);
}
