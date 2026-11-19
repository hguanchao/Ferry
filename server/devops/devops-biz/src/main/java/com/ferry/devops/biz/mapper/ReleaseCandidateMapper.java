package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.ReleaseCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReleaseCandidateMapper extends BaseMapper<ReleaseCandidate> {

    @org.apache.ibatis.annotations.Select("SELECT COUNT(1) FROM dev_release_candidate WHERE is_deleted = 0 AND status IN (2,3,4,5) AND id = #{rcId}")
    long countActive(@Param("rcId") Long rcId);

    /** 发布单可选制品：已晋级预发(env_category=2) 且门禁通过(1) 或有效豁免(4) */
    @org.apache.ibatis.annotations.Select("SELECT a.id AS artifactId, a.app_id AS appId, app.name AS appName, "
            + "a.version, a.digest, a.form, a.gate_conclusion AS gateConclusion, a.create_time AS createTime "
            + "FROM dev_artifact a JOIN dev_artifact_env ae ON ae.artifact_id = a.id AND ae.env_category = 2 "
            + "JOIN dev_app app ON app.id = a.app_id AND app.is_deleted = 0 "
            + "WHERE a.delete_seq = 0 AND a.gate_conclusion IN (1,4) AND a.app_id = #{appId} ORDER BY a.create_time DESC")
    java.util.List<java.util.Map<String, Object>> selectArtifactCandidates(@Param("appId") Long appId);

    /** 应用是否核心链路 */
    @org.apache.ibatis.annotations.Select("SELECT id, core_link AS coreLink FROM dev_app WHERE is_deleted = 0 AND id = #{appId}")
    com.ferry.devops.api.entity.App selectAppCore(@Param("appId") Long appId);
}
