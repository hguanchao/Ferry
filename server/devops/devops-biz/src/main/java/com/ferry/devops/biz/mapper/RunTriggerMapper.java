package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunTrigger;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流水线触发记录数据访问
 */
@Mapper
public interface RunTriggerMapper extends BaseMapper<RunTrigger> {
}
