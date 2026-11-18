package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunLogSegment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RunLogSegmentMapper extends BaseMapper<RunLogSegment> {

    /**
     * 按任务查日志分段
     */
    @Select("SELECT * FROM dev_run_log_segment WHERE task_id = #{taskId} ORDER BY segment_no")
    List<RunLogSegment> selectByTask(@Param("taskId") Long taskId);
}
