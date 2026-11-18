package com.ferry.devops.biz.mapper;
import java.util.Date;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.Run;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RunMapper extends BaseMapper<Run> {

    /**
     * 流水线在指定幂等键上是否已有活动运行（排队/运行/阻塞）
     */
    @Select("SELECT * FROM dev_run WHERE is_deleted = 0 AND idempotent_key = #{key} "
            + "AND status IN (1,2,3) ORDER BY id DESC LIMIT 1")
    Run selectActiveByIdemKey(@Param("key") String key);

    /**
     * 流水线在指定幂等键上是否已有成功运行
     */
    @Select("SELECT * FROM dev_run WHERE is_deleted = 0 AND idempotent_key = #{key} AND status = 4 ORDER BY id DESC LIMIT 1")
    Run selectSuccessByIdemKey(@Param("key") String key);

    /**
     * 同一流水线是否存在进行中的运行（排队/运行/阻塞）
     */
    @Select("SELECT COUNT(1) FROM dev_run WHERE is_deleted = 0 AND pipeline_id = #{pipelineId} AND status IN (1,2,3)")
    long countActiveByPipeline(@Param("pipelineId") Long pipelineId);

    /**
     * 应用最近 N 次触发次数（频率管控）
     */
    @Select("SELECT COUNT(1) FROM dev_run WHERE is_deleted = 0 AND app_id = #{appId} "
            + "AND trigger_user = #{userId} AND create_time > #{since}")
    long countByUserSince(@Param("appId") Long appId, @Param("userId") Long userId, @Param("since") java.util.Date since);

    /**
     * 生成运行编号：RUN-yyyyMMdd-当日序号（四位）
     */
    @Select("SELECT COUNT(1) + 1 FROM dev_run WHERE is_deleted = 0 AND run_no LIKE CONCAT('RUN-', #{day}, '-%')")
    long nextSeqOfDay(@Param("day") String day);

    /**
     * 应用历史运行出现过的分支
     */
    @Select("SELECT DISTINCT branch FROM dev_run WHERE is_deleted = 0 AND app_id = #{appId} ORDER BY branch")
    List<String> selectBranches(@Param("appId") Long appId);
}
