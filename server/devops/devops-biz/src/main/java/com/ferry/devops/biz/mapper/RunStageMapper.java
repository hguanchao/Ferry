package com.ferry.devops.biz.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunStage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RunStageMapper extends BaseMapper<RunStage> {

    /**
     * 按运行查阶段（按时序）
     */
    @Select("SELECT * FROM dev_run_stage WHERE is_deleted = 0 AND run_id = #{runId} ORDER BY sort")
    List<RunStage> selectByRun(@Param("runId") Long runId);

    /**
     * 按状态查阶段（按结束时间升序，供门禁调度批量拉取）
     */
    @Select("SELECT * FROM dev_run_stage WHERE is_deleted = 0 AND status = #{status} "
            + "ORDER BY end_time, id LIMIT #{limit}")
    List<RunStage> selectByStatusLimit(@Param("status") Integer status, @Param("limit") int limit);
}
