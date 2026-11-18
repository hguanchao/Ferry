package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.devops.api.dto.NotifyChannelDTO;
import com.ferry.devops.api.dto.NotifyTemplateDTO;
import com.ferry.devops.api.dto.query.NotifyChannelQuery;
import com.ferry.devops.api.dto.query.NotifyTemplateQuery;
import com.ferry.devops.api.entity.NotifyChannel;
import com.ferry.devops.api.entity.NotifyRecord;
import com.ferry.devops.api.entity.NotifyTemplate;
import com.ferry.devops.api.vo.NotifyChannelVO;
import com.ferry.devops.api.vo.NotifyTemplateVO;
import com.ferry.devops.biz.mapper.NotifyChannelMapper;
import com.ferry.devops.biz.mapper.NotifyRecordMapper;
import com.ferry.devops.biz.mapper.NotifyTemplateMapper;
import com.ferry.devops.biz.service.INotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 通知渠道与模板服务实现(最小实现：不真实发送，仅落记录)
 */
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl extends ServiceImpl<NotifyChannelMapper, NotifyChannel> implements INotifyService {
    private final NotifyTemplateMapper templateMapper;
    private final NotifyRecordMapper recordMapper;

    @Override
    public IPage<NotifyChannelVO> channelPage(NotifyChannelQuery query) {
        IPage<NotifyChannel> pages = baseMapper.selectPage(Page.create(query), Wrappers.<NotifyChannel>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getType()), NotifyChannel::getType, query.getType())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), NotifyChannel::getStatus, query.getStatus())
                .like(ObjUtil.isNotEmpty(query.getName()), NotifyChannel::getName, query.getName())
                .orderByDesc(NotifyChannel::getCreateTime));
        return convert(pages, NotifyChannelVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveChannel(NotifyChannelDTO body) {
        NotifyChannel channel = BeanUtil.copy(body, NotifyChannel.class);
        if (channel.getStatus() == null) {
            channel.setStatus(IntConst.ONE);
        }
        return ObjUtil.isEmpty(body.getId())
                ? baseMapper.insert(channel) > IntConst.ZERO
                : baseMapper.updateById(channel) > IntConst.ZERO;
    }

    @Override
    public IPage<NotifyTemplateVO> templatePage(NotifyTemplateQuery query) {
        IPage<NotifyTemplate> pages = templateMapper.selectPage(Page.create(query), Wrappers.<NotifyTemplate>lambdaQuery()
                .like(ObjUtil.isNotEmpty(query.getEvent()), NotifyTemplate::getEvent, query.getEvent())
                .eq(ObjUtil.isNotEmpty(query.getChannelType()), NotifyTemplate::getChannelType, query.getChannelType())
                .eq(ObjUtil.isNotEmpty(query.getIsEnabled()), NotifyTemplate::getIsEnabled, query.getIsEnabled())
                .orderByDesc(NotifyTemplate::getCreateTime));
        return convert(pages, NotifyTemplateVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveTemplate(NotifyTemplateDTO body) {
        NotifyTemplate template = BeanUtil.copy(body, NotifyTemplate.class);
        if (template.getIsEnabled() == null) {
            template.setIsEnabled(IntConst.ONE);
        }
        return ObjUtil.isEmpty(body.getId())
                ? templateMapper.insert(template) > IntConst.ZERO
                : templateMapper.updateById(template) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean testChannel(Long id) {
        NotifyChannel channel = baseMapper.selectById(id);
        if (channel == null) {
            throw new ServiceException("通知渠道不存在");
        }
        // 最小实现：写一条状态为成功的测试通知记录，不实际调用外部渠道
        NotifyRecord record = new NotifyRecord();
        record.setEvent("CHANNEL_TEST");
        record.setChannelId(id);
        record.setContent("渠道连通性测试消息");
        record.setStatus(2);
        record.setRetryCount(IntConst.ZERO);
        record.setSendTime(new Date());
        record.setCreateTime(new Date());
        return recordMapper.insert(record) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletedChannels(List<Long> ids) {
        return baseMapper.deleteBatchIds(ids) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletedTemplates(List<Long> ids) {
        return templateMapper.deleteBatchIds(ids) > IntConst.ZERO;
    }

    private <E, V> IPage<V> convert(IPage<E> pages, Class<V> clazz) {
        IPage<V> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        List<V> records = new java.util.ArrayList<>();
        for (E item : pages.getRecords()) {
            records.add(BeanUtil.copy(item, clazz));
        }
        result.setRecords(records);
        return result;
    }
}
