package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunTrigger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RunTriggerMapper extends BaseMapper<RunTrigger> {

    /**
     * 应用最近 20 条触发记录
     */
    @Select("SELECT * FROM dev_run_trigger WHERE app_id = #{appId} ORDER BY create_time DESC LIMIT 20")
    List<RunTrigger> selectRecentByApp(@Param("appId") Long appId);
}
