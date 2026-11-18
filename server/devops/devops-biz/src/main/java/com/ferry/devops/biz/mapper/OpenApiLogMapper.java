package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.OpenApiLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 开放 API 调用日志 Mapper
 */
@Mapper
public interface OpenApiLogMapper extends BaseMapper<OpenApiLog> {
}
