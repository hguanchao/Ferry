package com.ferry.system.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.system.api.dto.query.RegionQuery;
import com.ferry.system.api.entity.Region;
import com.ferry.system.api.vo.tree.RegionTree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RegionMapper extends BaseMapper<Region> {

    /**
     * 根据条件查询懒加载列表数据
     *
     * @param query 查询条件
     * @return 区域列表数据
     */
    List<RegionTree> selectLazyList(@Param("query") RegionQuery query);
}
