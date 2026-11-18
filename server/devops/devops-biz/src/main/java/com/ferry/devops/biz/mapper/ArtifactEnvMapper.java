package com.ferry.devops.biz.mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.ArtifactEnv;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArtifactEnvMapper extends BaseMapper<ArtifactEnv> {

    /**
     * 取制品的环境类别列表。
     */
    @Select("SELECT DISTINCT env_category FROM dev_artifact_env WHERE artifact_id = #{artifactId} ORDER BY env_category")
    java.util.List<Integer> categories(@Param("artifactId") Long artifactId);
}
