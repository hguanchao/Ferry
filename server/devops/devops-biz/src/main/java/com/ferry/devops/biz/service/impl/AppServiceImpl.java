package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.devops.api.dto.AppDTO;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.enums.RepoTypeEnum;
import com.ferry.devops.api.vo.AppVO;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.service.IAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements IAppService {

    @Override
    public IPage<AppVO> pageList(AppQuery body) {
        return baseMapper.selectPageList(Page.create(body), body);
    }

    @Override
    public AppVO detail(Long id) {
        App app = getById(id);
        if (ObjUtil.isEmpty(app)) {
            throw new ServiceException("应用不存在");
        }
        return BeanUtil.copy(app, AppVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(AppDTO body) {
        long count = baseMapper.selectCodeCount(body.getCode(), body.getId());
        if (count > IntConst.ZERO) {
            throw new ServiceException("应用编码已存在，请更换");
        }
        if (StrUtil.isNotBlank(body.getRepoType()) && RepoTypeEnum.of(body.getRepoType()) == null) {
            throw new ServiceException("代码托管平台取值不合法");
        }
        if (ObjUtil.isNotEmpty(body.getId())) {
            App app = getById(body.getId());
            if (ObjUtil.isEmpty(app)) {
                throw new ServiceException("应用不存在");
            }
            // 下游全部按编码引用，改动会让历史流水线、发布单指错应用
            if (!ObjUtil.equals(app.getCode(), body.getCode())) {
                throw new ServiceException("应用编码创建后不可修改");
            }
        }
        return saveOrUpdate(BeanUtil.copy(body, App.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的应用");
        }
        return removeByIds(ids);
    }
}
