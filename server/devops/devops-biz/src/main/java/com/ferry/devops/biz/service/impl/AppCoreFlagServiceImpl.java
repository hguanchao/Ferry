package com.ferry.devops.biz.service.impl;

import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.AppCoreFlagApplyDTO;
import com.ferry.devops.api.dto.AppCoreFlagApproveDTO;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.AppCoreFlagApply;
import com.ferry.devops.biz.mapper.AppCoreFlagApplyMapper;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.service.IAppCoreFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 核心链路变更服务实现
 */
@Service
@RequiredArgsConstructor
public class AppCoreFlagServiceImpl implements IAppCoreFlagService {
    private final AppCoreFlagApplyMapper applyMapper;
    private final AppMapper appMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean apply(AppCoreFlagApplyDTO body) {
        requireApp(body.getAppId());
        AppCoreFlagApply apply = new AppCoreFlagApply();
        apply.setAppId(body.getAppId());
        apply.setIsTargetCore(body.getIsTargetCore());
        apply.setReason(body.getReason());
        apply.setStatus(IntConst.ONE);
        apply.setApplicant(AuthUtil.getUserId());
        return applyMapper.insert(apply) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(AppCoreFlagApproveDTO body) {
        AppCoreFlagApply apply = applyMapper.selectById(body.getApplyId());
        if (apply == null || !ObjUtil.equals(apply.getStatus(), IntConst.ONE)) {
            throw new ServiceException("申请不存在或已处理");
        }
        apply.setApprover(AuthUtil.getUserId());
        apply.setApproveTime(new Date());
        apply.setStatus(Boolean.TRUE.equals(body.getPass()) ? 2 : 3);
        applyMapper.updateById(apply);
        if (Boolean.TRUE.equals(body.getPass())) {
            App app = appMapper.selectById(apply.getAppId());
            if (app != null) {
                app.setCoreLink(apply.getIsTargetCore());
                appMapper.updateById(app);
            }
        }
        return true;
    }

    @Override
    public AppCoreFlagApply get(Long applyId) {
        return applyMapper.selectById(applyId);
    }

    private void requireApp(Long appId) {
        if (appMapper.selectById(appId) == null) {
            throw new ServiceException("应用不存在");
        }
    }
}
