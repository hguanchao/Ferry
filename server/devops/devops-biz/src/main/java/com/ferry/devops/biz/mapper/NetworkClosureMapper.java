package com.ferry.devops.biz.mapper;
import java.util.Date;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.NetworkClosure;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NetworkClosureMapper extends BaseMapper<NetworkClosure> {

    @org.apache.ibatis.annotations.Select("SELECT COUNT(1) FROM dev_network_closure WHERE start_date <= #{today} AND end_date >= #{today} AND (scope = 1 OR (scope = 2 AND app_id = #{appId}))")
    long countCovering(@Param("today") java.util.Date today, @Param("appId") Long appId);
}
