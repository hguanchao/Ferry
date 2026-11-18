package com.ferry.devops.biz.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.Artifact;
import com.ferry.devops.api.entity.ArtifactEnv;
import com.ferry.devops.api.vo.TraceVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArtifactMapper extends BaseMapper<Artifact> {

    /**
     * 按(租户,应用,版本,未删)查重。
     */
    @Select("SELECT COUNT(1) FROM dev_artifact WHERE delete_seq = 0 AND app_id = #{appId} AND version = #{version}")
    long countByVersion(@Param("appId") Long appId, @Param("version") String version);

    /**
     * 按摘要反查有效制品。
     */
    @Select("SELECT COUNT(1) FROM dev_artifact WHERE delete_seq = 0 AND digest = #{digest}")
    long countByDigest(@Param("digest") String digest);

    /**
     * 查询制品所处的环境类别（去重）。
     */
    @Select("SELECT env_category FROM dev_artifact_env WHERE artifact_id = #{artifactId} ORDER BY env_category")
    List<Integer> envCategories(@Param("artifactId") Long artifactId);

    /**
     * 查询制品是否已晋级到某环境。
     */
    @Select("SELECT COUNT(1) FROM dev_artifact_env WHERE artifact_id = #{artifactId} AND env_id = #{envId}")
    long countEnv(@Param("artifactId") Long artifactId, @Param("envId") Long envId);

    /**
     * 取制品在指定类别的最近一条环境标记（用于找晋级来源）。
     */
    @Select("SELECT * FROM dev_artifact_env WHERE artifact_id = #{artifactId} AND env_category = #{category} "
            + "ORDER BY promoted_at DESC LIMIT 1")
    ArtifactEnv latestEnv(@Param("artifactId") Long artifactId, @Param("category") Integer category);

    /**
     * 反查：摘要/版本号/Commit 精确匹配（取未删）。
     */
    @Select("SELECT * FROM dev_artifact WHERE delete_seq = 0 AND "
            + "(digest = #{keyword} OR version = #{keyword} OR commit_id = #{keyword}) "
            + "ORDER BY create_time DESC LIMIT 20")
    List<Artifact> search(@Param("keyword") String keyword);

    /**
     * 追溯链路：关联发布单id。
     */
    @Select("SELECT DISTINCT rc_id FROM dev_release_artifact WHERE artifact_id = #{artifactId}")
    List<Long> traceReleaseIds(@Param("artifactId") Long artifactId);

    /**
     * 追溯链路：关联部署记录id。
     */
    @Select("SELECT DISTINCT id FROM dev_deploy_record WHERE is_deleted = 0 AND artifact_id = #{artifactId}")
    List<Long> traceDeployIds(@Param("artifactId") Long artifactId);

    /**
     * 追溯链路：关联回滚记录id。
     */
    @Select("SELECT DISTINCT id FROM dev_rollback_record WHERE is_deleted = 0 AND artifact_id = #{artifactId}")
    List<Long> traceRollbackIds(@Param("artifactId") Long artifactId);

    /**
     * 追溯基础信息。
     */
    @Select("SELECT id AS artifactId, digest, version, branch, commit_id AS commitId, builder, run_id AS runId "
            + "FROM dev_artifact WHERE id = #{artifactId} AND delete_seq = 0")
    TraceVO traceBase(@Param("artifactId") Long artifactId);

    /**
     * 清理候选：排除已删、待确认、生产生效，按构建时间倒序。
     */
    @Select("SELECT * FROM dev_artifact WHERE delete_seq = 0 AND app_id = #{appId} "
            + "AND confirm_status = 1 AND (prod_use_status IS NULL OR prod_use_status != 1) "
            + "ORDER BY create_time DESC")
    List<Artifact> cleanCandidates(@Param("appId") Long appId);

    /**
     * 曾被生产使用（部署记录中存在该制品且环境类别为生产）的制品id。
     */
    @Select("SELECT DISTINCT a.id FROM dev_artifact a "
            + "JOIN dev_deploy_record d ON d.artifact_id = a.id AND d.is_deleted = 0 "
            + "JOIN dev_environment e ON e.id = d.env_id AND e.is_deleted = 0 AND e.category = 3 "
            + "WHERE a.delete_seq = 0 AND a.app_id = #{appId}")
    List<Long> prodUsedArtifactIds(@Param("appId") Long appId);

    /**
     * 读取环境信息（应用id、类别、部署类型），用于晋级形态/来源校验。避免跨模块注入，按 id 直查。
     */
    @Select("SELECT id, app_id AS appId, category, deploy_type AS deployType, "
            + "current_artifact_id AS currentArtifactId FROM dev_environment "
            + "WHERE is_deleted = 0 AND id = #{envId}")
    java.util.Map<String, Object> envInfo(@Param("envId") Long envId);

    /**
     * 更新环境当前生效制品（晋级不等于部署；此处仅供部署成功后调用方使用，本服务晋级不改该字段，
     * 保留方法供同域审计/部署联动）。
     */
    @Update("UPDATE dev_environment SET current_artifact_id = #{artifactId} WHERE is_deleted = 0 AND id = #{envId}")
    int updateEnvCurrentArtifact(@Param("envId") Long envId, @Param("artifactId") Long artifactId);

    /**
     * 查应用测试环境id（入库后自动标记测试环境用）。
     */
    @Select("SELECT id FROM dev_environment WHERE is_deleted = 0 AND app_id = #{appId} AND category = 1 "
            + "ORDER BY id ASC LIMIT 1")
    Long testEnvId(@Param("appId") Long appId);

    /**
     * 部署时间线：按环境/批次/结果/耗时。
     */
    @Select("SELECT d.env_id AS envId, e.category AS envCategory, d.operator, d.start_time AS time, "
            + "CASE WHEN d.status = 4 THEN '成功' WHEN d.status = 5 THEN '失败' ELSE '进行中' END AS result "
            + "FROM dev_deploy_record d LEFT JOIN dev_environment e ON e.id = d.env_id "
            + "WHERE d.is_deleted = 0 AND d.artifact_id = #{artifactId} ORDER BY d.start_time DESC")
    List<java.util.Map<String, Object>> deployTimeline(@Param("artifactId") Long artifactId);
}
