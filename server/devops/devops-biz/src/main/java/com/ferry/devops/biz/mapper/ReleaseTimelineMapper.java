package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.ReleaseTimeline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReleaseTimelineMapper extends BaseMapper<ReleaseTimeline> {

    @org.apache.ibatis.annotations.Select("SELECT id, node, operator, remark, create_time AS createTime FROM dev_release_timeline WHERE rc_id = #{rcId} ORDER BY create_time")
    java.util.List<java.util.Map<String, Object>> selectByRc(@Param("rcId") Long rcId);
}
