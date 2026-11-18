package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.ReleaseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReleaseRecordMapper extends BaseMapper<ReleaseRecord> {

    @org.apache.ibatis.annotations.Select("SELECT COUNT(1) FROM dev_release_record WHERE is_deleted = 0 AND rc_id = #{rcId}")
    long countByRc(@Param("rcId") Long rcId);
}
