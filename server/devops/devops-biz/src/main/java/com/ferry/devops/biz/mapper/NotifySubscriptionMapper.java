package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.NotifySubscription;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知订阅 Mapper
 */
@Mapper
public interface NotifySubscriptionMapper extends BaseMapper<NotifySubscription> {
}
