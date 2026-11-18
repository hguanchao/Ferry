package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.PipelineVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PipelineVersionMapper extends BaseMapper<PipelineVersion> {

    /**
     * 查询流水线最大版本号
     */
    @Select("SELECT IFNULL(MAX(version_no), 0) FROM dev_pipeline_version WHERE pipeline_id = #{pipelineId}")
    Integer selectMaxVersionNo(@Param("pipelineId") Long pipelineId);

    /**
     * 按内容摘要查已有版本（去重）
     */
    @Select("SELECT id FROM dev_pipeline_version WHERE pipeline_id = #{pipelineId} "
            + "AND content_hash = #{hash} ORDER BY version_no DESC LIMIT 1")
    Long selectIdByHash(@Param("pipelineId") Long pipelineId, @Param("hash") String hash);
}
