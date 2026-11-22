package com.ferry.devops.biz.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.GateExemption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GateExemptionMapper extends BaseMapper<GateExemption> {

    /**
     * 查询运行下当前生效的豁免。到期判断在查询时进行（status=1 且 expire_at 未过）。
     */
    @Select("SELECT * FROM dev_gate_exemption WHERE run_id = #{runId} "
            + "AND status = 1 AND expire_at > NOW() ORDER BY create_time DESC")
    List<GateExemption> activeByRun(@Param("runId") Long runId);

    /**
     * 查询单条结果的有效豁免。
     */
    @Select("SELECT * FROM dev_gate_exemption WHERE run_stage_gate_id = #{runStageGateId} "
            + "AND status = 1 AND expire_at > NOW() ORDER BY create_time DESC LIMIT 1")
    GateExemption activeByRunStageGate(@Param("runStageGateId") Long runStageGateId);

    /**
     * 撤销同结果上的旧豁免（重复申请仅保留最后一次）。
     */
    @Update("UPDATE dev_gate_exemption SET status = 3 WHERE run_stage_gate_id = #{runStageGateId} AND status = 1")
    int revokeActive(@Param("runStageGateId") Long runStageGateId);

    /**
     * 查询已到期但仍为生效状态的豁免。
     */
    @Select("SELECT * FROM dev_gate_exemption WHERE status = 1 AND expire_at <= NOW() "
            + "ORDER BY expire_at LIMIT #{limit}")
    List<GateExemption> selectExpiredActive(@Param("limit") int limit);
}
