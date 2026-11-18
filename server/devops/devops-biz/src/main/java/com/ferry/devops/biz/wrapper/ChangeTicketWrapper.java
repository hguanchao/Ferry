package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.ChangeTicketQuery;
import com.ferry.devops.api.entity.ChangeTicket;
import com.ferry.devops.api.vo.ChangeTicketVO;

/**
 * 变更单列表查询与 VO 转换
 */
public class ChangeTicketWrapper extends BaseEntityWrapper<ChangeTicket, ChangeTicketVO> {

    public static ChangeTicketWrapper build() {
        return new ChangeTicketWrapper();
    }

    @Override
    public ChangeTicketVO vo(ChangeTicket entity) {
        return BeanUtil.copy(entity, ChangeTicketVO.class);
    }

    /**
     * 变更单查询条件
     */
    public LambdaQueryWrapper<ChangeTicket> lambda(ChangeTicketQuery query) {
        return Wrappers.<ChangeTicket>lambdaQuery()
                .like(StrUtil.isNotBlank(query.getTicketNo()), ChangeTicket::getTicketNo, query.getTicketNo())
                .like(StrUtil.isNotBlank(query.getKeyword()), ChangeTicket::getTitle, query.getKeyword())
                .eq(ObjUtil.isNotEmpty(query.getType()), ChangeTicket::getType, query.getType())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), ChangeTicket::getStatus, query.getStatus())
                .eq(ObjUtil.isNotEmpty(query.getApplicant()), ChangeTicket::getApplicant, query.getApplicant())
                .ge(StrUtil.isNotBlank(query.getWindowStart()), ChangeTicket::getWindowStart, query.getWindowStart())
                .le(StrUtil.isNotBlank(query.getWindowEnd()), ChangeTicket::getWindowEnd, query.getWindowEnd())
                .orderByDesc(ChangeTicket::getCreateTime);
    }
}
