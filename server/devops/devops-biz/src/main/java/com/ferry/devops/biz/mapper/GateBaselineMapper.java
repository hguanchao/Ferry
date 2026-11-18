package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.GateBaseline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GateBaselineMapper extends BaseMapper<GateBaseline> {

    /**
     * 取应用当前生效基线。
     */
    @Select("SELECT * FROM dev_gate_baseline WHERE app_id = #{appId} AND is_active = 1 "
            + "ORDER BY create_time DESC LIMIT 1")
    GateBaseline activeByApp(@Param("appId") Long appId);

    /**
     * 将该应用全部基线置失效（重设审批通过后）。
     */
    @Update("UPDATE dev_gate_baseline SET is_active = 0 WHERE app_id = #{appId}")
    int deactivateAll(@Param("appId") Long appId);
}
