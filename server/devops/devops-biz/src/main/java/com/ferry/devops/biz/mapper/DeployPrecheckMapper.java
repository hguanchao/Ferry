package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.DeployPrecheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeployPrecheckMapper extends BaseMapper<DeployPrecheck> {

    @org.apache.ibatis.annotations.Select("SELECT check_type AS checkType, conclusion, detail FROM dev_deploy_precheck WHERE deploy_id = #{deployId} ORDER BY check_type")
    java.util.List<java.util.Map<String, Object>> selectByDeploy(@Param("deployId") Long deployId);
}
