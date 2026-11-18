package com.ferry.devops.biz.mapper;
import java.util.Date;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.ActionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 动作记录 Mapper
 */
@Mapper
public interface ActionRecordMapper extends BaseMapper<ActionRecord> {

    /** 动作成功数 */
    @Select("SELECT COUNT(1) FROM dev_action_record WHERE is_deleted = 0 AND status = 5")
    long countSuccess();

    /** 动作撤销数 */
    @Select("SELECT COUNT(1) FROM dev_action_record WHERE is_deleted = 0 AND status = 7")
    long countUndo();

    /** 同应用时间窗内指定级别的动作数(频控) */
    @Select("SELECT COUNT(1) FROM dev_action_record WHERE is_deleted = 0 AND app_id = #{appId} "
            + "AND level = #{level} AND create_time >= #{since}")
    long countSince(@Param("appId") Long appId, @Param("level") Integer level, @Param("since") java.util.Date since);
}
