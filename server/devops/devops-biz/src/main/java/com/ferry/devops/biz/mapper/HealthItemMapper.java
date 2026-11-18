package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.HealthItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HealthItemMapper extends BaseMapper<HealthItem> {

    @org.apache.ibatis.annotations.Select("SELECT check_type AS checkType, threshold, sample_interval AS sampleInterval, consecutive, is_supported AS isSupported FROM dev_health_item WHERE env_id = #{envId}")
    java.util.List<java.util.Map<String, Object>> selectByEnv(@Param("envId") Long envId);
}
