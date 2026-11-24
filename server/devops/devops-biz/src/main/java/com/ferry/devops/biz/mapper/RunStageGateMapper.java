package com.ferry.devops.biz.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunStageGate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RunStageGateMapper extends BaseMapper<RunStageGate> {

    /**
     * 按运行查门禁结果，不通过与数据获取失败置顶。
     */
    @Select("SELECT * FROM dev_run_stage_gate WHERE run_id = #{runId} "
            + "ORDER BY CASE WHEN status IN (2,5) THEN 0 ELSE 1 END, id ASC")
    List<RunStageGate> listByRun(@Param("runId") Long runId);

    /**
     * 按运行阶段查门禁结果。
     */
    @Select("SELECT * FROM dev_run_stage_gate WHERE run_stage_id = #{runStageId} ORDER BY rule_type")
    List<RunStageGate> listByStage(@Param("runStageId") Long runStageId);

    /**
     * 统计运行下阻塞结果数量（不通过 2、数据获取失败 5）。
     */
    @Select("SELECT COUNT(1) FROM dev_run_stage_gate WHERE run_id = #{runId} AND status IN (2,5)")
    long countBlocking(@Param("runId") Long runId);

    /**
     * 统计运行阶段下阻塞结果数量。
     */
    @Select("SELECT COUNT(1) FROM dev_run_stage_gate WHERE run_stage_id = #{runStageId} AND status IN (2,5)")
    long countBlockingByStage(@Param("runStageId") Long runStageId);

    /**
     * 按唯一键写入或更新门禁结果。已豁免（4）的结论不被新报告覆盖。
     */
    @Insert("""
            INSERT INTO dev_run_stage_gate
                (id, run_id, run_stage_id, rule_id, rule_type, rule_version, rule_snapshot,
                 current_value, threshold, status, evidence, source, schema_version, fetch_error,
                 create_time, update_time)
            VALUES
                (#{id}, #{runId}, #{runStageId}, #{ruleId}, #{ruleType}, #{ruleVersion}, #{ruleSnapshot},
                 #{currentValue}, #{threshold}, #{status}, #{evidence}, #{source}, #{schemaVersion}, #{fetchError},
                 #{createTime}, #{updateTime})
            ON DUPLICATE KEY UPDATE
                run_id = VALUES(run_id),
                rule_id = VALUES(rule_id),
                rule_version = VALUES(rule_version),
                rule_snapshot = VALUES(rule_snapshot),
                current_value = VALUES(current_value),
                threshold = VALUES(threshold),
                status = IF(status = 4, status, VALUES(status)),
                evidence = VALUES(evidence),
                source = VALUES(source),
                schema_version = VALUES(schema_version),
                fetch_error = VALUES(fetch_error),
                update_time = VALUES(update_time)
            """)
    int upsert(RunStageGate result);

    /**
     * 将不通过结果改为已豁免。
     */
    @Update("UPDATE dev_run_stage_gate SET status = 4, update_time = NOW(3) "
            + "WHERE id = #{id} AND status = 2")
    int exempt(@Param("id") Long id);

    /**
     * 条件更新门禁状态（撤销豁免/到期恢复等）。
     */
    @Update("UPDATE dev_run_stage_gate SET status = #{status}, update_time = NOW(3) "
            + "WHERE id = #{id} AND status = #{expectedStatus}")
    int updateStatus(@Param("id") Long id, @Param("expectedStatus") Integer expectedStatus,
                     @Param("status") Integer status);
}
