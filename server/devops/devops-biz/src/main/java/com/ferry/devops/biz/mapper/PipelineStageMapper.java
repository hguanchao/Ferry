package com.ferry.devops.biz.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.PipelineStage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PipelineStageMapper extends BaseMapper<PipelineStage> {

    /**
     * 按流水线与版本查询阶段，按排序号升序
     */
    @Select("SELECT * FROM dev_pipeline_stage WHERE is_deleted = 0 AND pipeline_id = #{pipelineId} AND version = #{version} ORDER BY sort ASC")
    List<PipelineStage> selectByPipelineVersion(@Param("pipelineId") Long pipelineId, @Param("version") Integer version);

    /**
     * 查询流水线已存在的最大版本号
     */
    @Select("SELECT IFNULL(MAX(version), 0) FROM dev_pipeline_stage WHERE is_deleted = 0 AND pipeline_id = #{pipelineId}")
    int selectMaxVersion(@Param("pipelineId") Long pipelineId);

    /**
     * 查询流水线已有的版本号列表（降序）
     */
    @Select("SELECT DISTINCT version FROM dev_pipeline_stage WHERE is_deleted = 0 AND pipeline_id = #{pipelineId} ORDER BY version DESC")
    List<Integer> selectDistinctVersions(@Param("pipelineId") Long pipelineId);
}
