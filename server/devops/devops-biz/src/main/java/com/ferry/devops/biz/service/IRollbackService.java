package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.RollbackDTO;
import com.ferry.devops.api.dto.query.RollbackQuery;
import com.ferry.devops.api.entity.RollbackRecord;
import com.ferry.devops.api.vo.RollbackCandidateVO;
import com.ferry.devops.api.vo.RollbackVO;

import java.util.List;

/**
 * 回滚服务
 */
public interface IRollbackService extends IService<RollbackRecord> {

    /** 发起回滚，返回回滚记录id */
    Long start(RollbackDTO body);

    IPage<RollbackVO> pageList(RollbackQuery body);

    /** 健康版本候选（prod_use_status=1/2） */
    List<RollbackCandidateVO> candidates(Long envId);
}
