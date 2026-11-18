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
import com.ferry.devops.api.dto.EnvironmentDTO;
import com.ferry.devops.api.dto.EnvTypeChangeApplyDTO;
import com.ferry.devops.api.dto.EnvTypeChangeApproveDTO;
import com.ferry.devops.api.dto.query.EnvironmentQuery;
import com.ferry.devops.api.entity.Environment;
import com.ferry.devops.api.enums.DeployTypeEnum;
import com.ferry.devops.api.enums.EnvCategoryEnum;
import com.ferry.devops.api.vo.EnvironmentVO;
import com.ferry.devops.api.vo.EnvOptionVO;
import com.ferry.devops.biz.mapper.EnvironmentMapper;
import com.ferry.devops.biz.service.IEnvironmentService;
import com.ferry.devops.biz.wrapper.EnvironmentWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 环境管理服务实现
 */
@Service
@RequiredArgsConstructor
public class EnvironmentServiceImpl extends ServiceImpl<EnvironmentMapper, Environment> implements IEnvironmentService {

    /** 类型变更申请状态：待审批，复用 status 字段 */
    private static final int STATUS_CHANGE_PENDING = 2;

    @Override
    public IPage<EnvironmentVO> pageList(EnvironmentQuery body) {
        EnvironmentWrapper wrapper = EnvironmentWrapper.build();
        IPage<Environment> pages = page(Page.create(body), wrapper.lambda(body));
        return wrapper.pageVo(pages);
    }

    @Override
    public EnvironmentVO detail(Long id) {
        return BeanUtil.copy(requireEnv(id), EnvironmentVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(EnvironmentDTO body) {
        checkName(body.getAppId(), body.getName(), body.getId());
        if (!EnvCategoryEnum.contains(body.getCategory())) {
            throw new ServiceException("环境类别取值不合法");
        }
        if (!DeployTypeEnum.contains(body.getDeployType())) {
            throw new ServiceException("部署类型取值不合法");
        }
        Environment env = BeanUtil.copy(body, Environment.class);
        boolean isNew = ObjUtil.isEmpty(body.getId());
        // 生产类别自动置保护标记；新建生产环境需二次确认
        if (ObjUtil.equals(body.getCategory(), EnvCategoryEnum.PRODUCTION.getCode())) {
            if (isNew && !Boolean.TRUE.equals(body.getProdConfirm())) {
                throw new ServiceException("生产环境需二次确认后方可创建");
            }
            env.setIsProtected(IntConst.ONE);
        } else {
            env.setIsProtected(IntConst.ZERO);
        }
        // 编辑时不允许通过本接口直接改部署类型，须走变更申请
        if (ObjUtil.isNotEmpty(body.getId())) {
            Environment old = requireEnv(body.getId());
            if (!ObjUtil.equals(old.getDeployType(), body.getDeployType())) {
                throw new ServiceException("部署类型变更需走变更申请");
            }
        }
        if (StrUtil.isBlank(env.getTargetConfig())) {
            env.setTargetConfig("{}");
        }
        return saveOrUpdate(env);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的环境");
        }
        for (Long id : ids) {
            if (baseMapper.countActiveDeploy(id) > IntConst.ZERO) {
                throw new ServiceException("环境存在进行中的部署，不可删除");
            }
            if (baseMapper.countCurrentArtifact(id) > IntConst.ZERO) {
                throw new ServiceException("环境存在生效制品，不可删除");
            }
        }
        return removeByIds(ids);
    }

    @Override
    public List<EnvOptionVO> options(Long appId) {
        List<Environment> envs = list(Wrappers.<Environment>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(appId), Environment::getAppId, appId)
                .orderByAsc(Environment::getName));
        List<EnvOptionVO> result = new java.util.ArrayList<>();
        for (Environment env : envs) {
            EnvOptionVO vo = new EnvOptionVO();
            vo.setValue(env.getId());
            vo.setLabel(env.getName());
            EnvCategoryEnum category = EnvCategoryEnum.of(env.getCategory());
            vo.setCode(category == null ? null : category.getDesc());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyTypeChange(EnvTypeChangeApplyDTO body) {
        Environment env = requireEnv(body.getId());
        if (!DeployTypeEnum.contains(body.getDeployType())) {
            throw new ServiceException("部署类型取值不合法");
        }
        if (ObjUtil.equals(env.getDeployType(), body.getDeployType())) {
            throw new ServiceException("目标部署类型与当前一致，无需变更");
        }
        if (ObjUtil.equals(env.getStatus(), STATUS_CHANGE_PENDING)) {
            throw new ServiceException("已有待审批的部署类型变更申请");
        }
        env.setStatus(STATUS_CHANGE_PENDING);
        return updateById(env);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveTypeChange(EnvTypeChangeApproveDTO body) {
        Environment env = requireEnv(body.getId());
        if (!ObjUtil.equals(env.getStatus(), STATUS_CHANGE_PENDING)) {
            throw new ServiceException("变更申请不存在或已处理");
        }
        if (!DeployTypeEnum.contains(body.getDeployType())) {
            throw new ServiceException("部署类型取值不合法");
        }
        if (Boolean.TRUE.equals(body.getPass())) {
            env.setDeployType(body.getDeployType());
            // 变更部署类型后清理历史版本指针，避免回滚到形态不匹配的产物
            env.setCurrentArtifactId(null);
        }
        env.setStatus(IntConst.ONE);
        return updateById(env);
    }

    @Override
    public Map<String, String> optionMap(Long appId) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Environment env : list(Wrappers.<Environment>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(appId), Environment::getAppId, appId)
                .orderByAsc(Environment::getName))) {
            result.put(String.valueOf(env.getId()), env.getName());
        }
        return result;
    }

    private Environment requireEnv(Long id) {
        Environment env = getById(id);
        if (env == null) {
            throw new ServiceException("环境不存在");
        }
        return env;
    }

    private void checkName(Long appId, String name, Long id) {
        long count = count(Wrappers.<Environment>lambdaQuery()
                .eq(Environment::getAppId, appId)
                .eq(Environment::getName, name)
                .ne(ObjUtil.isNotEmpty(id), Environment::getId, id));
        if (count > IntConst.ZERO) {
            throw new ServiceException("同一应用下环境名称已存在");
        }
    }
}
