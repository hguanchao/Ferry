package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.PipelineTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PipelineTemplateMapper extends BaseMapper<PipelineTemplate> {
}
