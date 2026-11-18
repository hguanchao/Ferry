package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.GateBaselineReset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GateBaselineResetMapper extends BaseMapper<GateBaselineReset> {

    /**
     * 取应用待审批的重设申请。
     */
    @Select("SELECT * FROM dev_gate_baseline_reset WHERE app_id = #{appId} AND status = 1 "
            + "ORDER BY create_time DESC LIMIT 1")
    GateBaselineReset pending(@Param("appId") Long appId);
}
