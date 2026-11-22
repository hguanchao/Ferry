package com.ferry.devops.biz.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunStageTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RunStageTaskMapper extends BaseMapper<RunStageTask> {

    /**
     * 按运行阶段查任务
     */
    @Select("SELECT * FROM dev_run_stage_task WHERE is_deleted = 0 AND run_stage_id = #{runStageId} ORDER BY sort")
    List<RunStageTask> selectByStage(@Param("runStageId") Long runStageId);

    /**
     * 按运行查任务
     */
    @Select("SELECT * FROM dev_run_stage_task WHERE is_deleted = 0 AND run_id = #{runId} ORDER BY sort")
    List<RunStageTask> selectByRun(@Param("runId") Long runId);
}
