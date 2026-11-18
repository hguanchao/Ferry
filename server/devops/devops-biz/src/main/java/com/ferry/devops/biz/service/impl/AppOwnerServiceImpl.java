package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.entity.AppOwner;
import com.ferry.devops.biz.mapper.AppOwnerMapper;
import com.ferry.devops.biz.service.IAppOwnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 应用负责人服务实现
 */
@Service
@RequiredArgsConstructor
public class AppOwnerServiceImpl extends ServiceImpl<AppOwnerMapper, AppOwner> implements IAppOwnerService {

    @Override
    public List<Long> ownerIds(Long appId) {
        return list(Wrappers.<AppOwner>lambdaQuery()
                        .eq(AppOwner::getAppId, appId).eq(AppOwner::getType, IntConst.ONE))
                .stream().map(AppOwner::getUserId).collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void replace(Long appId, String ownerIds, String backupIds) {
        baseMapper.delete(Wrappers.<AppOwner>lambdaQuery().eq(AppOwner::getAppId, appId));
        int sort = 0;
        for (Long userId : CollUtil.toLongList(ownerIds)) {
            AppOwner owner = new AppOwner();
            owner.setAppId(appId);
            owner.setUserId(userId);
            owner.setType(IntConst.ONE);
            owner.setSort(sort++);
            baseMapper.insert(owner);
        }
        for (Long userId : CollUtil.toLongList(backupIds)) {
            AppOwner owner = new AppOwner();
            owner.setAppId(appId);
            owner.setUserId(userId);
            owner.setType(2);
            owner.setSort(sort++);
            baseMapper.insert(owner);
        }
    }

    @Override
    public String ownerNames(Long appId, String ownerIds) {
        List<AppOwner> owners = list(Wrappers.<AppOwner>lambdaQuery()
                .eq(AppOwner::getAppId, appId).eq(AppOwner::getType, IntConst.ONE));
        Map<Long, String> names = new LinkedHashMap<>();
        for (AppOwner owner : owners) {
            names.put(owner.getUserId(), "用户" + owner.getUserId());
        }
        StringBuilder builder = new StringBuilder();
        for (Long id : CollUtil.toLongList(ownerIds)) {
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(names.getOrDefault(id, ""));
        }
        return builder.toString();
    }
}
