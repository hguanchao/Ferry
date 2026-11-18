package com.ferry.devops.biz.mapper;
import java.util.Date;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.MetricDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 每日指标 Mapper
 */
@Mapper
public interface MetricDailyMapper extends BaseMapper<MetricDaily> {

    /** 跑批应用清单：取近 30 天有部署记录的应用 */
    @Select("SELECT DISTINCT app_id FROM dev_deploy_record WHERE is_deleted = 0 AND app_id IS NOT NULL")
    List<Long> listActiveAppIds();

    /** 统计指定应用前一日成功部署数(跑批最小实现的数据来源) */
    @Select("SELECT COUNT(1) FROM dev_deploy_record WHERE is_deleted = 0 AND app_id = #{appId} "
            + "AND status = 5 AND start_time >= #{start} AND start_time < #{end}")
    long countSuccessDeploy(@Param("appId") Long appId, @Param("start") java.util.Date start,
                            @Param("end") java.util.Date end);
}
