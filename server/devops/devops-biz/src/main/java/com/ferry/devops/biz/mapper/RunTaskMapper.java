package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RunTaskMapper extends BaseMapper<RunTask> {

    /**
     * 按阶段查任务
     */
    @Select("SELECT * FROM dev_run_task WHERE is_deleted = 0 AND stage_id = #{stageId} ORDER BY id")
    List<RunTask> selectByStage(@Param("stageId") Long stageId);

    /**
     * 按运行查任务
     */
    @Select("SELECT * FROM dev_run_task WHERE is_deleted = 0 AND run_id = #{runId} ORDER BY id")
    List<RunTask> selectByRun(@Param("runId") Long runId);
}
