package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.NotifyChannel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知渠道 Mapper
 */
@Mapper
public interface NotifyChannelMapper extends BaseMapper<NotifyChannel> {
}
