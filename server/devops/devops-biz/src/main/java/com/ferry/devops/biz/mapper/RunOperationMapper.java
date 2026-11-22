package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunOperation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RunOperationMapper extends BaseMapper<RunOperation> {

    /**
     * 查询运行最近一条干预记录，用于区分人工暂停/继续/中止。
     */
    @Select("SELECT * FROM dev_run_operation WHERE run_id = #{runId} "
            + "ORDER BY create_time DESC, id DESC LIMIT 1")
    RunOperation selectLatestByRun(@Param("runId") Long runId);
}
