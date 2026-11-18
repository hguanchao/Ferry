package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.ExportTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 导出任务 Mapper
 */
@Mapper
public interface ExportTaskMapper extends BaseMapper<ExportTask> {
}
