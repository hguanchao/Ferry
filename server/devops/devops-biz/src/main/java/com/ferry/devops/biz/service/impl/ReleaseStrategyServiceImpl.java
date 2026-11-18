package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.dto.NetworkClosureDTO;
import com.ferry.devops.api.dto.ReleaseStrategyDTO;
import com.ferry.devops.api.entity.NetworkClosure;
import com.ferry.devops.api.entity.ReleaseStrategy;
import com.ferry.devops.api.vo.NetworkClosureVO;
import com.ferry.devops.api.vo.ReleaseStrategyVO;
import com.ferry.devops.biz.mapper.NetworkClosureMapper;
import com.ferry.devops.biz.mapper.ReleaseStrategyMapper;
import com.ferry.devops.biz.service.IReleaseStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 发布策略与封网期服务实现
 */
@Service
@RequiredArgsConstructor
public class ReleaseStrategyServiceImpl extends ServiceImpl<ReleaseStrategyMapper, ReleaseStrategy>
        implements IReleaseStrategyService {

    private final NetworkClosureMapper closureMapper;

    @Override
    public ReleaseStrategyVO detail(Long appId) {
        ReleaseStrategy strategy = getOne(Wrappers.<ReleaseStrategy>lambdaQuery()
                .eq(ReleaseStrategy::getAppId, appId));
        return strategy == null ? null : BeanUtil.copy(strategy, ReleaseStrategyVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ReleaseStrategyDTO body) {
        ReleaseStrategy strategy = getOne(Wrappers.<ReleaseStrategy>lambdaQuery()
                .eq(ReleaseStrategy::getAppId, body.getAppId()));
        ReleaseStrategy entity = BeanUtil.copy(body, ReleaseStrategy.class);
        if (ObjUtil.isNotEmpty(entity.getId()) || strategy != null) {
            if (strategy != null) {
                entity.setId(strategy.getId());
            }
            return updateById(entity);
        }
        entity.setIsClosureExcluded(body.getIsClosureExcluded() == null ? IntConst.ZERO : body.getIsClosureExcluded());
        return save(entity);
    }

    @Override
    public List<NetworkClosureVO> closureList() {
        return BeanUtil.copy(closureMapper.selectList(Wrappers.<NetworkClosure>lambdaQuery()
                .orderByDesc(NetworkClosure::getStartDate)), NetworkClosureVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveClosure(NetworkClosureDTO body) {
        NetworkClosure closure = BeanUtil.copy(body, NetworkClosure.class);
        if (closure.getId() == null) {
            return closureMapper.insert(closure) > 0;
        }
        return closureMapper.updateById(closure) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletedClosure(List<Long> ids) {
        return closureMapper.deleteBatchIds(ids) > IntConst.ZERO;
    }

    @Override
    public boolean isClosed(Date date, Long appId) {
        return closureMapper.countCovering(date, appId == null ? IntConst.MINUS : appId) > IntConst.ZERO;
    }
}
