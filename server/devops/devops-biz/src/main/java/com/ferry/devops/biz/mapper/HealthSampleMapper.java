package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.HealthSample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HealthSampleMapper extends BaseMapper<HealthSample> {

    @org.apache.ibatis.annotations.Select("SELECT id, deploy_id AS deployId, batch_id AS batchId, check_type AS checkType, value, threshold, conclusion, sampled_at AS sampledAt FROM dev_health_sample WHERE is_deleted = 0 AND deploy_id = #{deployId} AND (#{batchId} IS NULL OR batch_id = #{batchId}) ORDER BY sampled_at")
    java.util.List<java.util.Map<String, Object>> selectByDeploy(@Param("deployId") Long deployId, @Param("batchId") Long batchId);
}
