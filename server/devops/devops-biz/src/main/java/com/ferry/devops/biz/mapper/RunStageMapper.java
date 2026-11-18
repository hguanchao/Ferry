package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunStage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RunStageMapper extends BaseMapper<RunStage> {

    /**
     * 按运行查阶段（按时序）
     */
    @Select("SELECT * FROM dev_run_stage WHERE is_deleted = 0 AND run_id = #{runId} ORDER BY seq")
    List<RunStage> selectByRun(@Param("runId") Long runId);
}
