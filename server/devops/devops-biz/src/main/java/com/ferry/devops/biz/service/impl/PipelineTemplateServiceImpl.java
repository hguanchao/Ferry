package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.AuthException;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.http.HttpStatus;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.model.AuthUser;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.PipelineTemplateDTO;
import com.ferry.devops.api.dto.PipelineTemplateVersionDTO;
import com.ferry.devops.api.dto.PipelineTemplateDefinitionDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.dto.query.PipelineTemplateQuery;
import com.ferry.devops.api.entity.PipelineTemplate;
import com.ferry.devops.api.entity.PipelineTemplateVersion;
import com.ferry.devops.api.enums.PipelineTemplateScopeEnum;
import com.ferry.devops.api.enums.PipelineTemplateStatusEnum;
import com.ferry.devops.api.vo.PipelineTemplateDefinitionVO;
import com.ferry.devops.api.vo.PipelineTemplateVO;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.mapper.PipelineTemplateMapper;
import com.ferry.devops.biz.service.IPipelineTemplateService;
import com.ferry.devops.biz.service.IPipelineTemplateVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PipelineTemplateServiceImpl extends ServiceImpl<PipelineTemplateMapper, PipelineTemplate> implements IPipelineTemplateService {
    private final PipelineMapper pipelineMapper;
    private final IPipelineTemplateVersionService pipelineTemplateVersionService;

    @Override
    public IPage<PipelineTemplateVO> pageList(PipelineTemplateQuery body) {
        if (!PipelineTemplateScopeEnum.contains(body.getScope())) {
            throw new ServiceException("请选择模板层级");
        }
        IPage<PipelineTemplateVO> page = Page.create(body);
        if (PipelineTemplateScopeEnum.ORG.getCode().equals(body.getScope())) {
            return baseMapper.selectOrgPageList(page, body);
        }
        return baseMapper.selectTeamPageList(page, body);
    }

    @Override
    public PipelineTemplateVO detail(Long id) {
        PipelineTemplate template = getById(id);
        if (ObjUtil.isEmpty(template)) {
            throw new ServiceException("模板不存在");
        }
        checkReadable(template);
        PipelineTemplateVO vo = BeanUtil.copy(template, PipelineTemplateVO.class);
        if (ObjUtil.isNotEmpty(template.getCurrentVersionId())) {
            PipelineTemplateVersion version = pipelineTemplateVersionService.getById(template.getCurrentVersionId());
            if (ObjUtil.isEmpty(version)) {
                throw new ServiceException("当前版本不存在，请联系管理员");
            }
            vo.setDefinition(JsonUtil.toBean(version.getContent(), PipelineTemplateDefinitionVO.class));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PipelineTemplateDTO body) {
        if (!PipelineTemplateScopeEnum.contains(body.getScope())) {
            throw new ServiceException("模板层级不合法：" + body.getScope());
        }
        if (ObjUtil.isEmpty(body.getId())) {
            return create(body);
        }
        return update(body);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的模板");
        }
        List<PipelineTemplate> templates = listByIds(ids);
        if (CollUtil.isEmpty(templates)) {
            throw new ServiceException("模板不存在");
        }
        for (PipelineTemplate template : templates) {
            checkManageable(template);
        }
        // 删除前置：已有派生流水线的模板只能停用，聚合一条查询判断
        List<Long> derivedIds = baseMapper.selectDerivedTemplateIds(ids);
        if (CollUtil.isNotEmpty(derivedIds)) {
            String names = templates.stream()
                    .filter(item -> derivedIds.contains(item.getId()))
                    .map(PipelineTemplate::getName)
                    .collect(Collectors.joining("、"));
            throw new ServiceException("模板「" + names + "」已有派生流水线，只能停用不能删除");
        }
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeStatus(Long id, Integer status) {
        if (id == null) {
            throw new ServiceException("模板id不能为空");
        }
        if (!PipelineTemplateStatusEnum.contains(status)) {
            throw new ServiceException("模板状态不合法");
        }
        PipelineTemplate template = getById(id);
        if (ObjUtil.isEmpty(template)) {
            throw new ServiceException("模板不存在");
        }
        checkManageable(template);
        if (ObjUtil.equals(template.getStatus(), status)) {
            return true;
        }
        return lambdaUpdate()
                .eq(PipelineTemplate::getId, template.getId())
                .set(PipelineTemplate::getStatus, status)
                .update();
    }

    @Override
    public IPage<PipelineVO> derivedList(PipelineTemplateQuery body) {
        if (body.getId() == null) {
            throw new ServiceException("模板id不能为空");
        }
        PipelineTemplate template = getById(body.getId());
        if (ObjUtil.isEmpty(template)) {
            throw new ServiceException("模板不存在");
        }
        checkReadable(template);
        // 复用流水线列表查询，按来源模板过滤；数据权限口径与流水线列表一致
        PipelineQuery query = new PipelineQuery();
        query.setSourceTemplateId(template.getId());
        return pipelineMapper.selectPageList(Page.create(body), query);
    }

    @Override
    public List<PipelineTemplateVO> options(String keyword) {
        List<PipelineTemplateVO> merged = new ArrayList<>(baseMapper.selectOrgOptions(keyword));
        merged.addAll(baseMapper.selectTeamOptions(keyword));
        merged.sort(Comparator.comparing(PipelineTemplateVO::getUpdateTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return merged.stream().limit(50).toList();
    }

    /**
     * 新增模板：校验层级维护权限与名称唯一后落库，并在同一事务内生成初始版本
     *
     * @param body 表单数据
     * @return true 成功 || false 失败
     */
    private boolean create(PipelineTemplateDTO body) {
        checkScopeManageable(body.getScope(), body.getDeptId());
        validateDefinition(body.getDefinition());
        validateNameUnique(body.getScope(), body.getDeptId(), body.getName(), null);

        PipelineTemplate template = BeanUtil.copy(body, PipelineTemplate.class);
        template.setStatus(PipelineTemplateStatusEnum.ENABLED.getCode());
        template.setStageCount(IntConst.ZERO);
        template.setVariableCount(IntConst.ZERO);
        template.setCreateUserName(currentUserName());
        boolean isSuccess = save(template);
        if (isSuccess) {
            PipelineTemplateVersionDTO versionDTO = new PipelineTemplateVersionDTO();
            versionDTO.setTemplateId(template.getId());
            versionDTO.setDefinition(body.getDefinition());
            versionDTO.setChangeNote(body.getChangeNote());
            pipelineTemplateVersionService.save(versionDTO);
        }
        return isSuccess;
    }

    /**
     * 编辑模板：层级与归属部门不可改，只允许改名称与适用场景
     *
     * @param body 表单数据
     * @return true 成功 || false 失败
     */
    private boolean update(PipelineTemplateDTO body) {
        PipelineTemplate template = getById(body.getId());
        if (ObjUtil.isEmpty(template)) {
            throw new ServiceException("模板不存在");
        }
        checkManageable(template);
        if (!ObjUtil.equals(template.getScope(), body.getScope())) {
            throw new ServiceException("模板层级创建后不可修改");
        }
        if (PipelineTemplateScopeEnum.TEAM.getCode().equals(template.getScope())
                && !ObjUtil.equals(template.getDeptId(), body.getDeptId())) {
            throw new ServiceException("归属部门创建后不可修改");
        }
        validateNameUnique(template.getScope(), template.getDeptId(), body.getName(), template.getId());

        template.setName(body.getName());
        template.setDescription(body.getDescription());
        return updateById(template);
    }

    /**
     * 校验新增时的层级维护权限：组织级仅平台管理员，团队级归属部门必须在数据权限范围内
     *
     * @param scope  模板层级
     * @param deptId 归属部门id
     */
    private void checkScopeManageable(Integer scope, Long deptId) {
        if (PipelineTemplateScopeEnum.ORG.getCode().equals(scope)) {
            AuthUser user = AuthUtil.getUser();
            if (user == null || !IntConst.ONE.equals(user.getIsSuper())) {
                throw new ServiceException("组织级模板仅平台管理员可维护");
            }
            return;
        }
        if (ObjUtil.isEmpty(deptId)) {
            throw new ServiceException("团队级模板请选择归属部门");
        }
        AuthUser user = AuthUtil.getUser();
        if (user == null || IntConst.ONE.equals(user.getIsSuper()) || Boolean.TRUE.equals(user.getDataAll())) {
            return;
        }
        Set<Long> deptIds = user.getDataDeptIds();
        if (deptIds == null || !deptIds.contains(deptId)) {
            throw new ServiceException("团队级模板只能归属你有数据权限的部门");
        }
    }

    /**
     * 校验对已有模板的维护权限：组织级仅平台管理员，团队级要求在可见范围内
     *
     * @param template 模板
     */
    private void checkManageable(PipelineTemplate template) {
        if (PipelineTemplateScopeEnum.ORG.getCode().equals(template.getScope())) {
            AuthUser user = AuthUtil.getUser();
            if (user == null || !IntConst.ONE.equals(user.getIsSuper())) {
                throw new ServiceException("组织级模板仅平台管理员可维护");
            }
            return;
        }
        checkReadable(template);
    }

    /**
     * 校验对已有模板的可见权限：组织级全员可见，团队级要求在数据权限范围内或本人创建
     *
     * @param template 模板
     */
    private void checkReadable(PipelineTemplate template) {
        if (PipelineTemplateScopeEnum.ORG.getCode().equals(template.getScope())) {
            return;
        }
        AuthUser user = AuthUtil.getUser();
        if (user == null || IntConst.ONE.equals(user.getIsSuper()) || Boolean.TRUE.equals(user.getDataAll())) {
            return;
        }
        boolean readable = (user.getDataDeptIds() != null && user.getDataDeptIds().contains(template.getDeptId()))
                || (Boolean.TRUE.equals(user.getDataSelf()) && ObjUtil.equals(user.getId(), template.getCreateUser()));
        if (!readable) {
            throw new AuthException(HttpStatus.FORBIDDEN);
        }
    }

    /**
     * 校验同层级下模板名称唯一，团队级再限定同部门
     *
     * @param scope     模板层级
     * @param deptId    归属部门id
     * @param name      模板名称
     * @param excludeId 编辑时要排除的模板id，新增时传 null
     */
    private void validateNameUnique(Integer scope, Long deptId, String name, Long excludeId) {
        long count = count(Wrappers.<PipelineTemplate>lambdaQuery()
                .eq(PipelineTemplate::getScope, scope)
                .eq(PipelineTemplateScopeEnum.TEAM.getCode().equals(scope), PipelineTemplate::getDeptId, deptId)
                .eq(PipelineTemplate::getName, name)
                .ne(ObjUtil.isNotEmpty(excludeId), PipelineTemplate::getId, excludeId));
        if (count > IntConst.ZERO) {
            throw new ServiceException(PipelineTemplateScopeEnum.TEAM.getCode().equals(scope)
                    ? "同部门下模板名称已存在" : "组织级模板名称已存在");
        }
    }

    /**
     * 校验模板定义
     *
     * @param definition 模板定义
     */
    private void validateDefinition(PipelineTemplateDefinitionDTO definition) {
        if (definition == null) {
            throw new ServiceException("至少配置一个阶段");
        }
        definition.validate();
    }

    /**
     * 当前登录人名称快照，优先昵称
     *
     * @return 名称，未登录时返回 null
     */
    private String currentUserName() {
        AuthUser user = AuthUtil.getUser();
        if (ObjUtil.isEmpty(user)) {
            return null;
        }
        return StrUtil.isNotBlank(user.getNickname()) ? user.getNickname() : user.getAccount();
    }
}
