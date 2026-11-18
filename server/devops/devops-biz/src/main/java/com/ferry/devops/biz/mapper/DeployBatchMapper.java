package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.DeployBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeployBatchMapper extends BaseMapper<DeployBatch> {

    @org.apache.ibatis.annotations.Select("SELECT id, batch_no AS batchNo, target_count AS targetCount, status, health_result AS healthResult, start_time AS startTime, end_time AS endTime FROM dev_deploy_batch WHERE is_deleted = 0 AND deploy_id = #{deployId} ORDER BY batch_no")
    java.util.List<java.util.Map<String, Object>> selectByDeploy(@Param("deployId") Long deployId);
}
