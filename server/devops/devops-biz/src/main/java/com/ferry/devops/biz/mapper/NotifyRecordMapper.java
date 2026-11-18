package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.NotifyRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知记录 Mapper
 */
@Mapper
public interface NotifyRecordMapper extends BaseMapper<NotifyRecord> {
}
