package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.RunStage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运行阶段数据访问
 */
@Mapper
public interface RunStageMapper extends BaseMapper<RunStage> {
}
