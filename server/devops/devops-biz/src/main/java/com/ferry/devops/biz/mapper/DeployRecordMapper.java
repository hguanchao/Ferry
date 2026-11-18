package com.ferry.devops.biz.mapper;
import java.util.Date;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.DeployRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface DeployRecordMapper extends BaseMapper<DeployRecord> {

    /**
     * 环境是否空闲（无进行中的部署）
     */
    @Select("SELECT COUNT(1) FROM dev_deploy_record WHERE is_deleted = 0 AND env_id = #{envId} AND status IN (1,2,3)")
    long countActiveDeploy(@Param("envId") Long envId);

    /**
     * 查询环境类别与部署类型
     */
    @Select("SELECT category, deploy_type AS deployType, current_artifact_id AS currentArtifactId "
            + "FROM dev_environment WHERE is_deleted = 0 AND id = #{envId}")
    Map<String, Object> selectEnvInfo(@Param("envId") Long envId);

    /**
     * 查询应用名称
     */
    @Select("SELECT name FROM dev_app WHERE is_deleted = 0 AND id = #{appId}")
    String selectAppName(@Param("appId") Long appId);

    /**
     * 查询环境名称
     */
    @Select("SELECT name FROM dev_environment WHERE is_deleted = 0 AND id = #{envId}")
    String selectEnvName(@Param("envId") Long envId);

    /**
     * 查询制品简要信息（存在/形态/使用状态）
     */
    @Select("SELECT id, form, prod_use_status AS prodUseStatus, confirm_status AS confirmStatus, "
            + "version, digest FROM dev_artifact WHERE delete_seq = 0 AND id = #{artifactId}")
    Map<String, Object> selectArtifactInfo(@Param("artifactId") Long artifactId);

    /**
     * 制品是否已晋级到目标环境
     */
    @Select("SELECT COUNT(1) FROM dev_artifact_env WHERE artifact_id = #{artifactId} AND env_id = #{envId}")
    long countArtifactEnv(@Param("artifactId") Long artifactId, @Param("envId") Long envId);

    /**
     * 查询目标环境当前生效制品的版本号（依赖兼容校验用）
     */
    @Select("SELECT a.version FROM dev_environment e JOIN dev_artifact a ON a.id = e.current_artifact_id "
            + "AND a.delete_seq = 0 WHERE e.is_deleted = 0 AND e.id = #{envId}")
    String selectCurrentArtifactVersion(@Param("envId") Long envId);

    /**
     * 查询已批准的变更单在指定时间是否覆盖窗口
     */
    @Select("SELECT COUNT(1) FROM dev_change_ticket WHERE is_deleted = 0 AND status = 3 AND id = #{ticketId} "
            + "AND window_start <= #{windowStart} AND window_end >= #{windowEnd}")
    long countApprovedTicket(@Param("ticketId") Long ticketId, @Param("windowStart") java.util.Date windowStart,
                             @Param("windowEnd") java.util.Date windowEnd);

    /**
     * 查询应用依赖数量（依赖兼容校验用）
     */
    @Select("SELECT COUNT(1) FROM dev_app_dependency WHERE is_deleted = 0 AND app_id = #{appId}")
    long countDependencies(@Param("appId") Long appId);
}
