package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.StageStat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 阶段耗时统计 Mapper
 */
@Mapper
public interface StageStatMapper extends BaseMapper<StageStat> {
}
