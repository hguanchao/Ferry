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
import com.ferry.devops.api.dto.AppDTO;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.vo.AppDetailVO;
import com.ferry.devops.api.vo.AppOptionVO;
import com.ferry.devops.api.vo.AppVO;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.service.IAppDependencyService;
import com.ferry.devops.biz.service.IAppOwnerService;
import com.ferry.devops.biz.service.IAppRepoService;
import com.ferry.devops.biz.service.IAppService;
import com.ferry.devops.biz.wrapper.AppWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 应用档案服务实现
 */
@Service
@RequiredArgsConstructor
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements IAppService {
    private final IAppOwnerService ownerService;
    private final IAppRepoService repoService;
    private final IAppDependencyService dependencyService;

    @Override
    public IPage<AppVO> pageList(AppQuery body) {
        AppWrapper wrapper = AppWrapper.build();
        IPage<App> pages = page(Page.create(body), wrapper.lambda(body));
        IPage<AppVO> result = wrapper.pageVo(pages);
        for (AppVO vo : result.getRecords()) {
            vo.setOwnerNames(ownerService.ownerNames(vo.getId(), vo.getOwnerIds()));
        }
        return result;
    }

    @Override
    public AppDetailVO detail(Long id) {
        App app = requireApp(id);
        AppDetailVO vo = BeanUtil.copy(app, AppDetailVO.class);
        vo.setRepos(repoService.repoList(id));
        vo.setOwners(BeanUtil.copy(ownerService.list(Wrappers.<com.ferry.devops.api.entity.AppOwner>lambdaQuery()
                .eq(com.ferry.devops.api.entity.AppOwner::getAppId, id)
                .orderByAsc(com.ferry.devops.api.entity.AppOwner::getSort)),
                com.ferry.devops.api.vo.AppOwnerVO.class));
        vo.setDependencies(dependencyService.dependencyList(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(AppDTO body) {
        checkCode(body.getCode(), body.getId());
        List<Long> owners = CollUtil.toLongList(body.getOwnerIds());
        if (owners.size() < IntConst.ONE || owners.size() > 3) {
            throw new ServiceException("应用负责人需 1-3 人");
        }
        if (ObjUtil.isNotEmpty(body.getId())) {
            App old = requireApp(body.getId());
            if (!ObjUtil.equals(old.getCode(), body.getCode())) {
                throw new ServiceException("应用编码创建后不可修改");
            }
        }
        App app = BeanUtil.copy(body, App.class);
        boolean success = saveOrUpdate(app);
        ownerService.replace(app.getId(), body.getOwnerIds(), body.getBackupOwnerIds());
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的应用");
        }
        for (Long id : ids) {
            if (baseMapper.countActiveRun(id) > IntConst.ZERO) {
                throw new ServiceException("应用存在进行中的运行，不可删除");
            }
            if (baseMapper.countActiveDeploy(id) > IntConst.ZERO) {
                throw new ServiceException("应用存在进行中的部署，不可删除");
            }
            if (baseMapper.countOpenRelease(id) > IntConst.ZERO) {
                throw new ServiceException("应用存在未关闭的发布单，不可删除");
            }
        }
        return removeByIds(ids);
    }

    @Override
    public List<AppOptionVO> options(String keyword, Integer scope) {
        return list(Wrappers.<App>lambdaQuery()
                .like(StrUtil.isNotBlank(keyword), App::getName, keyword)
                .orderByAsc(App::getName)).stream().map(app -> {
            AppOptionVO vo = new AppOptionVO();
            vo.setValue(app.getId());
            vo.setLabel(app.getName());
            vo.setCode(app.getCode());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, String> optionMap() {
        Map<String, String> result = new LinkedHashMap<>();
        for (App app : list(Wrappers.<App>lambdaQuery().orderByAsc(App::getName))) {
            result.put(String.valueOf(app.getId()), app.getName());
        }
        return result;
    }

    @Override
    public void checkCode(String code, Long id) {
        long count = count(Wrappers.<App>lambdaQuery()
                .eq(App::getCode, code)
                .ne(ObjUtil.isNotEmpty(id), App::getId, id));
        if (count > IntConst.ZERO) {
            throw new ServiceException("应用编码已存在，请更换");
        }
        if (!code.matches("[a-z0-9-]{2,40}")) {
            throw new ServiceException("应用编码只能包含小写字母、数字和中划线，长度 2-40");
        }
    }

    @Override
    public boolean saveOrUpdateApp(App app) {
        return saveOrUpdate(app);
    }

    private App requireApp(Long id) {
        App app = getById(id);
        if (app == null) {
            throw new ServiceException("应用不存在");
        }
        return app;
    }
}
