package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.AiEvalSample;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 评测样本 Mapper
 */
@Mapper
public interface AiEvalSampleMapper extends BaseMapper<AiEvalSample> {
}
