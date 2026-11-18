package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.ReleaseCheckItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReleaseCheckItemMapper extends BaseMapper<ReleaseCheckItem> {

    @org.apache.ibatis.annotations.Select("SELECT id, check_type AS checkType, system_conclusion AS systemConclusion, manual_conclusion AS manualConclusion, remark, evidence FROM dev_release_check_item WHERE rc_id = #{rcId} ORDER BY check_type")
    java.util.List<java.util.Map<String, Object>> selectByRc(@Param("rcId") Long rcId);

    @org.apache.ibatis.annotations.Select("SELECT COUNT(1) FROM dev_release_check_item WHERE rc_id = #{rcId} AND (manual_conclusion IS NULL OR system_conclusion = 2 AND manual_conclusion IS NULL)")
    long countUnfinished(@Param("rcId") Long rcId);
}
