package com.ferry.devops.biz.service;
import java.util.Date;

import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.NetworkClosureDTO;
import com.ferry.devops.api.dto.ReleaseStrategyDTO;
import com.ferry.devops.api.entity.ReleaseStrategy;
import com.ferry.devops.api.vo.NetworkClosureVO;
import com.ferry.devops.api.vo.ReleaseStrategyVO;

import java.util.List;

/**
 * 发布策略与封网期服务
 */
public interface IReleaseStrategyService extends IService<ReleaseStrategy> {

    ReleaseStrategyVO detail(Long appId);

    boolean save(ReleaseStrategyDTO body);

    List<NetworkClosureVO> closureList();

    boolean saveClosure(NetworkClosureDTO body);

    boolean deletedClosure(List<Long> ids);

    /** 指定日期是否被封网（全局或指定应用） */
    boolean isClosed(java.util.Date date, Long appId);
}
