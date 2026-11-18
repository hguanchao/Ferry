package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.ChangeTicketDTO;
import com.ferry.devops.api.dto.query.ChangeTicketQuery;
import com.ferry.devops.api.entity.ChangeTicket;
import com.ferry.devops.api.entity.ChangeTicketApp;
import com.ferry.devops.api.enums.ChangeTicketTypeEnum;
import com.ferry.devops.api.enums.TicketStatusEnum;
import com.ferry.devops.api.vo.ChangeTicketVO;
import com.ferry.devops.biz.mapper.ChangeTicketAppMapper;
import com.ferry.devops.biz.mapper.ChangeTicketMapper;
import com.ferry.devops.biz.service.IChangeTicketService;
import com.ferry.devops.biz.wrapper.ChangeTicketWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;

/**
 * 变更单服务实现
 */
@Service
@RequiredArgsConstructor
public class ChangeTicketServiceImpl extends ServiceImpl<ChangeTicketMapper, ChangeTicket> implements IChangeTicketService {

    private final ChangeTicketAppMapper ticketAppMapper;

    @Override
    public IPage<ChangeTicketVO> pageList(ChangeTicketQuery body) {
        ChangeTicketWrapper wrapper = ChangeTicketWrapper.build();
        return wrapper.pageVo(page(Page.create(body), wrapper.lambda(body)));
    }

    @Override
    public ChangeTicketVO detail(Long id) {
        return BeanUtil.copy(require(id), ChangeTicketVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ChangeTicketDTO body) {
        ChangeTicket ticket = BeanUtil.copy(body, ChangeTicket.class);
        if (ObjUtil.isEmpty(ticket.getId())) {
            ticket.setTicketNo("CH-" + com.ferry.common.core.utils.DateUtil.format(new Date(), "yyyyMMdd")
                    + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            ticket.setStatus(TicketStatusEnum.DRAFT.getCode());
            ticket.setApplicant(AuthUtil.getUserId());
            ticket.setSource(IntConst.ONE);
        }
        boolean ok = saveOrUpdate(ticket);
        // 关联应用
        ticketAppMapper.delete(Wrappers.<ChangeTicketApp>lambdaQuery().eq(ChangeTicketApp::getTicketId, ticket.getId()));
        for (Long appId : CollUtil.toLongList(body.getAppIds())) {
            ChangeTicketApp rel = new ChangeTicketApp();
            rel.setTicketId(ticket.getId());
            rel.setAppId(appId);
            ticketAppMapper.insert(rel);
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(Long id) {
        ChangeTicket ticket = require(id);
        if (!ObjUtil.equals(ticket.getStatus(), TicketStatusEnum.DRAFT.getCode())) {
            throw new ServiceException("仅草稿可提交审批");
        }
        ticket.setStatus(TicketStatusEnum.PENDING_APPROVAL.getCode());
        return updateById(ticket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long id, Boolean pass, String opinion) {
        ChangeTicket ticket = require(id);
        if (!ObjUtil.equals(ticket.getStatus(), TicketStatusEnum.PENDING_APPROVAL.getCode())) {
            throw new ServiceException("变更单不在待审批状态");
        }
        // 紧急单需技术负责人：简化为任意审批人（由 :approve 权限约束）
        if (ObjUtil.equals(ticket.getType(), ChangeTicketTypeEnum.EMERGENCY.getCode())
                && StrUtil.isBlank(opinion)) {
            throw new ServiceException("紧急变更单审批需填写意见");
        }
        ticket.setApprover(AuthUtil.getUserId());
        ticket.setStatus(Boolean.TRUE.equals(pass)
                ? TicketStatusEnum.APPROVED.getCode() : TicketStatusEnum.DRAFT.getCode());
        return updateById(ticket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean close(Long id) {
        ChangeTicket ticket = require(id);
        ticket.setStatus(TicketStatusEnum.CLOSED.getCode());
        return updateById(ticket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long id, String reason) {
        ChangeTicket ticket = require(id);
        ticket.setStatus(TicketStatusEnum.CANCELLED.getCode());
        return updateById(ticket);
    }

    private ChangeTicket require(Long id) {
        ChangeTicket ticket = getById(id);
        if (ticket == null) {
            throw new ServiceException("变更单不存在");
        }
        return ticket;
    }
}
