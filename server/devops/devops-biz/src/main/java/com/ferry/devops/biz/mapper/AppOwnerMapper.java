package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.AppOwner;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppOwnerMapper extends BaseMapper<AppOwner> {
}
