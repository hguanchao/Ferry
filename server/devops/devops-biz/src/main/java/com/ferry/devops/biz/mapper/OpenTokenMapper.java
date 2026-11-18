package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.OpenToken;
import org.apache.ibatis.annotations.Mapper;

/**
 * 开放 API Token Mapper
 */
@Mapper
public interface OpenTokenMapper extends BaseMapper<OpenToken> {
}
