package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RollbackRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RollbackRecordMapper extends BaseMapper<RollbackRecord> {

    @org.apache.ibatis.annotations.Select("SELECT e.name AS envName, a.version AS artifactVersion FROM dev_environment e LEFT JOIN dev_artifact a ON a.id = #{artifactId} AND a.delete_seq = 0 WHERE e.id = #{envId}")
    java.util.Map<String, Object> selectEnvArtifact(@Param("envId") Long envId, @Param("artifactId") Long artifactId);

    /** 健康版本候选：prod_use_status 1 当前生效 / 2 历史版本，按创建时间倒序 */
    @org.apache.ibatis.annotations.Select("SELECT id AS artifactId, version, digest, prod_use_status AS prodUseStatus, create_time AS createTime "
            + "FROM dev_artifact WHERE delete_seq = 0 AND app_id = #{appId} AND prod_use_status IN (1,2) ORDER BY create_time DESC")
    java.util.List<java.util.Map<String, Object>> selectHealthyCandidates(@Param("appId") Long appId);
}
