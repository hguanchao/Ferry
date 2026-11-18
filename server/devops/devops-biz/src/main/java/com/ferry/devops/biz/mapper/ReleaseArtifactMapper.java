package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.ReleaseArtifact;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReleaseArtifactMapper extends BaseMapper<ReleaseArtifact> {

    @org.apache.ibatis.annotations.Select("SELECT ra.id, ra.app_id AS appId, ra.artifact_id AS artifactId, ra.gate_conclusion AS gateConclusion, ra.is_locked AS isLocked, a.version, a.digest, a.form, app.name AS appName FROM dev_release_artifact ra LEFT JOIN dev_artifact a ON a.id = ra.artifact_id AND a.delete_seq = 0 LEFT JOIN dev_app app ON app.id = ra.app_id WHERE ra.rc_id = #{rcId}")
    java.util.List<java.util.Map<String, Object>> selectByRc(@Param("rcId") Long rcId);

    @org.apache.ibatis.annotations.Select("SELECT COUNT(1) FROM dev_release_artifact WHERE rc_id = #{rcId} AND app_id = #{appId}")
    long countApp(@Param("rcId") Long rcId, @Param("appId") Long appId);
}
