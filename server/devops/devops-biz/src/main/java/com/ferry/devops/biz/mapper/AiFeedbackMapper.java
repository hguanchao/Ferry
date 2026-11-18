package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.AiFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 反馈 Mapper
 */
@Mapper
public interface AiFeedbackMapper extends BaseMapper<AiFeedback> {
}
