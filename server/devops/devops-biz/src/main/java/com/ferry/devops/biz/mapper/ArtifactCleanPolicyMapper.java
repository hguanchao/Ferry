package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.ArtifactCleanPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArtifactCleanPolicyMapper extends BaseMapper<ArtifactCleanPolicy> {

    /**
     * 按应用查策略（含全局 0）。
     */
    @Select("SELECT * FROM dev_artifact_clean_policy WHERE app_id = #{appId} LIMIT 1")
    ArtifactCleanPolicy findByApp(@Param("appId") Long appId);
}
