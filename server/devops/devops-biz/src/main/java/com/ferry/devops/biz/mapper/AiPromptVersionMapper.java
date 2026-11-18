package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.AiPromptVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 提示版本 Mapper
 */
@Mapper
public interface AiPromptVersionMapper extends BaseMapper<AiPromptVersion> {
}
