package com.ferry.system.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.system.api.dto.query.TenantQuery;
import com.ferry.system.api.entity.Tenant;
import com.ferry.system.api.vo.TenantVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

    /**
     * 根据查询条件获取租户分页列表
     *
     * @param page  分页对象
     * @param query 查询条件
     * @return 租户分页列表
     */
    IPage<TenantVO> selectPageList(IPage<Tenant> page, @Param("query") TenantQuery query);
}