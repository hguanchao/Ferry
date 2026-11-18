package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.NotifyChannelDTO;
import com.ferry.devops.api.dto.NotifyTemplateDTO;
import com.ferry.devops.api.dto.query.NotifyChannelQuery;
import com.ferry.devops.api.dto.query.NotifyTemplateQuery;
import com.ferry.devops.api.entity.NotifyChannel;
import com.ferry.devops.api.vo.NotifyChannelVO;
import com.ferry.devops.api.vo.NotifyTemplateVO;

import java.util.List;

/**
 * 通知渠道与模板服务
 */
public interface INotifyService extends IService<NotifyChannel> {

    IPage<NotifyChannelVO> channelPage(NotifyChannelQuery query);

    boolean saveChannel(NotifyChannelDTO body);

    IPage<NotifyTemplateVO> templatePage(NotifyTemplateQuery query);

    boolean saveTemplate(NotifyTemplateDTO body);

    /** 发送测试消息(最小实现：仅落一条待发通知记录) */
    boolean testChannel(Long id);

    boolean deletedChannels(List<Long> ids);

    boolean deletedTemplates(List<Long> ids);
}
