package com.ferry.devops.biz.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.PipelineStageTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PipelineStageTaskMapper extends BaseMapper<PipelineStageTask> {

    /**
     * 按阶段查询任务，按排序号升序
     */
    @Select("SELECT * FROM dev_pipeline_stage_task WHERE is_deleted = 0 AND stage_id = #{stageId} ORDER BY sort ASC")
    List<PipelineStageTask> selectByStage(@Param("stageId") Long stageId);

    /**
     * 按阶段批量查询任务（避免 N+1），按阶段与排序号升序
     */
    @Select("<script>SELECT * FROM dev_pipeline_stage_task WHERE is_deleted = 0 AND stage_id IN "
            + "<foreach collection='stageIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY stage_id ASC, sort ASC</script>")
    List<PipelineStageTask> selectByStageIds(@Param("stageIds") List<Long> stageIds);
}
