package com.ferry.devops.biz.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.GateBaselineIssue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GateBaselineIssueMapper extends BaseMapper<GateBaselineIssue> {

    /**
     * 取基线全部问题指纹。
     */
    @Select("SELECT fingerprint FROM dev_gate_baseline_issue WHERE baseline_id = #{baselineId}")
    List<String> fingerprints(@Param("baselineId") Long baselineId);

    /**
     * 统计基线问题各状态数量。
     */
    @Select("SELECT status, COUNT(1) AS cnt FROM dev_gate_baseline_issue WHERE baseline_id = #{baselineId} GROUP BY status")
    List<java.util.Map<String, Object>> statusStats(@Param("baselineId") Long baselineId);
}
