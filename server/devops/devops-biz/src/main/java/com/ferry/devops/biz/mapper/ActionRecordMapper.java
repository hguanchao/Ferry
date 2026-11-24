package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.ActionRecord;
import org.apache.ibatis.annotations.Mapper;
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
}
