package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.ChangeTicketDTO;
import com.ferry.devops.api.dto.query.ChangeTicketQuery;
import com.ferry.devops.api.entity.ChangeTicket;
import com.ferry.devops.api.vo.ChangeTicketVO;

/**
 * 变更单服务
 */
public interface IChangeTicketService extends IService<ChangeTicket> {

    IPage<ChangeTicketVO> pageList(ChangeTicketQuery body);

    ChangeTicketVO detail(Long id);

    boolean save(ChangeTicketDTO body);

    boolean submit(Long id);

    boolean approve(Long id, Boolean pass, String opinion);

    boolean close(Long id);

    boolean cancel(Long id, String reason);
}
