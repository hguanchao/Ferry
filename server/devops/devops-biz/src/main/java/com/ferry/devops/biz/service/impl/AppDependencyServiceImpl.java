package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.dto.AppDependencyDTO;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.AppDependency;
import com.ferry.devops.api.vo.AppDependencyVO;
import com.ferry.devops.biz.mapper.AppDependencyMapper;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.service.IAppDependencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用依赖服务服务实现
 */
@Service
@RequiredArgsConstructor
public class AppDependencyServiceImpl extends ServiceImpl<AppDependencyMapper, AppDependency> implements IAppDependencyService {
    private final AppMapper appMapper;

    @Override
    public List<AppDependencyVO> dependencyList(Long appId) {
        return BeanUtil.copy(baseMapper.selectList(Wrappers.<AppDependency>lambdaQuery()
                .eq(AppDependency::getAppId, appId)), AppDependencyVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDependency(AppDependencyDTO body) {
        requireApp(body.getAppId());
        AppDependency dependency = BeanUtil.copy(body, AppDependency.class);
        if (ObjUtil.isEmpty(dependency.getId())) {
            return baseMapper.insert(dependency) > IntConst.ZERO;
        }
        return baseMapper.updateById(dependency) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletedDependency(List<Long> ids) {
        return baseMapper.deleteBatchIds(ids) > IntConst.ZERO;
    }

    private void requireApp(Long appId) {
        App app = appMapper.selectById(appId);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
    }
}
