package com.ferry.devops.biz.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.GateResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GateResultMapper extends BaseMapper<GateResult> {

    /**
     * 运行下全部门禁结果，不通过与数据获取失败置顶，再按 id。
     */
    @Select("SELECT * FROM dev_gate_result WHERE run_id = #{runId} "
            + "ORDER BY CASE WHEN conclusion IN (2,5) THEN 0 ELSE 1 END, id ASC")
    List<GateResult> listByRun(@Param("runId") Long runId);

    /**
     * 统计运行下阻塞结论数量（不通过 2、数据获取失败 5）。
     */
    @Select("SELECT COUNT(1) FROM dev_gate_result WHERE run_id = #{runId} AND conclusion IN (2,5)")
    long countBlocking(@Param("runId") Long runId);

    /**
     * 将单条结果结论改为已豁免。
     */
    @Update("UPDATE dev_gate_result SET conclusion = 4 WHERE id = #{id} AND conclusion = 2")
    int exempt(@Param("id") Long id);
}
