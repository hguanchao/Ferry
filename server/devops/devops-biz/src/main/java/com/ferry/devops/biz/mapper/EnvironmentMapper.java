package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.Environment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EnvironmentMapper extends BaseMapper<Environment> {

    /**
     * 环境是否存在进行中的部署（状态 1未开始 2执行中 3观察中）
     */
    @Select("SELECT COUNT(1) FROM dev_deploy_record WHERE is_deleted = 0 AND env_id = #{envId} AND status IN (1,2,3)")
    long countActiveDeploy(@Param("envId") Long envId);

    /**
     * 环境是否存在生效制品
     */
    @Select("SELECT COUNT(1) FROM dev_environment WHERE is_deleted = 0 AND id = #{envId} AND current_artifact_id IS NOT NULL")
    long countCurrentArtifact(@Param("envId") Long envId);
}
