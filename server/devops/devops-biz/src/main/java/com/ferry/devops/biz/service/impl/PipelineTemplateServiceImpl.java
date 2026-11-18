package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.CryptoUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.PipelineTemplateDTO;
import com.ferry.devops.api.dto.PipelineTemplateVersionDTO;
import com.ferry.devops.api.dto.TemplateVariableDTO;
import com.ferry.devops.api.dto.query.TemplateQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.PipelineTemplate;
import com.ferry.devops.api.entity.PipelineTemplateDerivation;
import com.ferry.devops.api.entity.PipelineTemplateVar;
import com.ferry.devops.api.entity.PipelineTemplateVersion;
import com.ferry.devops.api.entity.PipelineVersion;
import com.ferry.devops.api.vo.DerivationVO;
import com.ferry.devops.api.vo.DiffVO;
import com.ferry.devops.api.vo.PipelineTemplateVO;
import com.ferry.devops.api.vo.PipelineTemplateVersionVO;
import com.ferry.devops.api.vo.TemplateVariableVO;
import com.ferry.devops.api.vo.VersionVO;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.mapper.PipelineTemplateMapper;
import com.ferry.devops.biz.mapper.PipelineTemplateDerivationMapper;
import com.ferry.devops.biz.mapper.PipelineTemplateVarMapper;
import com.ferry.devops.biz.mapper.PipelineTemplateVersionMapper;
import com.ferry.devops.biz.mapper.PipelineVersionMapper;
import com.ferry.devops.biz.service.IPipelineTemplateService;
import com.ferry.devops.biz.wrapper.PipelineTemplateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模板服务实现：版本管理、派生关系、逐条合并、有派生只停用不删除。
 */
@Service
@RequiredArgsConstructor
public class PipelineTemplateServiceImpl extends ServiceImpl<PipelineTemplateMapper, PipelineTemplate>
        implements IPipelineTemplateService {
    private final PipelineTemplateVersionMapper versionMapper;
    private final PipelineTemplateVarMapper varMapper;
    private final PipelineTemplateDerivationMapper derivationMapper;
    private final PipelineVersionMapper pipelineVersionMapper;
    private final PipelineMapper pipelineMapper;

    @Override
    public IPage<PipelineTemplateVO> pageList(TemplateQuery body) {
        PipelineTemplateWrapper wrapper = PipelineTemplateWrapper.build(versionMapper);
        IPage<PipelineTemplate> pages = page(Page.create(body), wrapper.lambda(body));
        IPage<PipelineTemplateVO> result = wrapper.pageVo(pages);
        result.setRecords(wrapper.voWithExtras(pages.getRecords()));
        return result;
    }

    @Override
    public PipelineTemplateVersionVO detail(Long id) {
        PipelineTemplate template = requireTemplate(id);
        PipelineTemplateVersion version = versionMapper.selectById(template.getCurrentVersionId());
        PipelineTemplateVersionVO vo = new PipelineTemplateVersionVO();
        if (version != null) {
            vo = BeanUtil.copy(version, PipelineTemplateVersionVO.class);
        }
        vo.setVariables(variables(template.getCurrentVersionId()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PipelineTemplateDTO body) {
        PipelineTemplate template;
        boolean isNew = ObjUtil.isEmpty(body.getId());
        if (isNew) {
            if (Integer.valueOf(1).equals(body.getScope()) && !AuthUtil.isSuper()) {
                throw new ServiceException("组织级模板仅平台管理员可建");
            }
            if (Integer.valueOf(2).equals(body.getScope()) && ObjUtil.isEmpty(body.getTeamId())) {
                throw new ServiceException("团队级模板须指定团队");
            }
            template = new PipelineTemplate();
            template.setIsEnabled(IntConst.ONE);
            template.setDerivedCount(IntConst.ZERO);
        } else {
            template = requireTemplate(body.getId());
        }
        template.setScope(body.getScope());
        template.setTeamId(body.getTeamId());
        template.setName(body.getName());
        template.setScene(body.getScene());
        if (ObjUtil.isNotEmpty(body.getIsEnabled())) {
            template.setIsEnabled(body.getIsEnabled());
        }
        boolean success = saveOrUpdate(template);
        // 定义或变量变更时生成新版本
        if (StrUtil.isNotBlank(body.getDefinition()) || CollUtil.isNotEmpty(body.getVariables())) {
            String contentJson = StrUtil.isNotBlank(body.getDefinition()) ? body.getDefinition() : currentJson(template);
            checkVariablesDeclared(contentJson, body.getVariables());
            PipelineTemplateVersion version = createVersion(template.getId(), contentJson,
                    ObjUtil.isNotEmpty(body.getSourceVersionId()) ? 3 : 1,
                    body.getChangeNote(), body.getSourceVersionId());
            saveVars(version.getId(), body.getVariables());
            template.setCurrentVersionId(version.getId());
            updateById(template);
            // 发新版：派生关系仅标记落后，不自动改定义
            derivationMapper.markBehindByTemplate(template.getId());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的模板");
        }
        for (Long id : ids) {
            if (derivationMapper.countByTemplate(id) > IntConst.ZERO) {
                throw new ServiceException("模板存在派生流水线，不可删除，仅可停用");
            }
        }
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enable(Long id) {
        PipelineTemplate template = requireTemplate(id);
        template.setIsEnabled(IntConst.ONE);
        return updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(Long id, String reason) {
        PipelineTemplate template = requireTemplate(id);
        template.setIsEnabled(IntConst.ZERO);
        return updateById(template);
    }

    @Override
    public IPage<VersionVO> versionPage(Long templateId, Long current, Long size) {
        IPage<PipelineTemplateVersion> pages = versionMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        current == null ? 1 : current, size == null ? 20 : size),
                Wrappers.<PipelineTemplateVersion>lambdaQuery()
                        .eq(PipelineTemplateVersion::getTemplateId, templateId)
                        .orderByDesc(PipelineTemplateVersion::getVersionNo));
        IPage<VersionVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        result.setRecords(pages.getRecords().stream().map(v -> BeanUtil.copy(v, VersionVO.class)).collect(Collectors.toList()));
        return result;
    }

    @Override
    public DiffVO versionDiff(Long leftId, Long rightId) {
        PipelineTemplateVersion left = versionMapper.selectById(leftId);
        PipelineTemplateVersion right = versionMapper.selectById(rightId);
        if (left == null || right == null) {
            throw new ServiceException("对比版本不存在");
        }
        DiffVO vo = new DiffVO();
        vo.setLeft(left.getContentJson());
        vo.setRight(right.getContentJson());
        vo.setLines(lineDiff(left.getContentJson(), right.getContentJson()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VersionVO rollback(PipelineTemplateVersionDTO body) {
        PipelineTemplateVersion target = versionMapper.selectById(body.getVersionId());
        PipelineTemplate template = requireTemplate(body.getTemplateId());
        if (target == null || !ObjUtil.equals(target.getTemplateId(), template.getId())) {
            throw new ServiceException("回退版本不存在");
        }
        PipelineTemplateVersion version = createVersion(template.getId(), target.getContentJson(), 3,
                body.getChangeNote(), target.getId());
        template.setCurrentVersionId(version.getId());
        updateById(template);
        derivationMapper.markBehindByTemplate(template.getId());
        return BeanUtil.copy(version, VersionVO.class);
    }

    @Override
    public List<DerivationVO> derivations(Long id) {
        requireTemplate(id);
        List<PipelineTemplateDerivation> rows = derivationMapper.selectList(Wrappers.<PipelineTemplateDerivation>lambdaQuery()
                .eq(PipelineTemplateDerivation::getTemplateId, id)
                .orderByDesc(PipelineTemplateDerivation::getId));
        List<DerivationVO> result = new ArrayList<>();
        for (PipelineTemplateDerivation row : rows) {
            DerivationVO vo = BeanUtil.copy(row, DerivationVO.class);
            Pipeline pipeline = pipelineMapper.selectById(row.getPipelineId());
            if (pipeline != null) {
                vo.setPipelineName(pipeline.getName());
                vo.setAppId(pipeline.getAppId());
                vo.setAppName(pipelineMapper.selectAppName(pipeline.getAppId()));
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VersionVO merge(PipelineTemplateVersionDTO body) {
        Pipeline pipeline = pipelineMapper.selectById(body.getPipelineId());
        PipelineTemplateVersion templateVersion = versionMapper.selectById(
                body.getTemplateVersionId() != null ? body.getTemplateVersionId() : body.getVersionId());
        if (pipeline == null || templateVersion == null) {
            throw new ServiceException("流水线或模板版本不存在");
        }
        String content = templateVersion.getContentJson();
        if (CollUtil.isNotEmpty(body.getItems())) {
            content = applyMergeItems(currentPipelineJson(pipeline), body.getItems());
        }
        String hash = CryptoUtil.sha256Hex(content);
        int versionNo = pipelineVersionMapper.selectMaxVersionNo(pipeline.getId()) + 1;
        PipelineVersion version = new PipelineVersion();
        version.setPipelineId(pipeline.getId());
        version.setVersionNo(versionNo);
        version.setContentJson(content);
        version.setContentHash(hash);
        version.setChangeNote(StrUtil.isBlank(body.getChangeNote()) ? "模板合并" : body.getChangeNote());
        version.setSourceType(4);
        pipelineVersionMapper.insert(version);
        pipeline.setCurrentVersionId(version.getId());
        pipelineMapper.updateById(pipeline);
        // 更新派生关系：合并后不再落后
        PipelineTemplateDerivation derivation = derivationMapper.selectByPipeline(pipeline.getId());
        if (derivation != null) {
            derivation.setTemplateVersionId(templateVersion.getId());
            derivation.setMergedVersionId(version.getId());
            derivation.setIsBehind(IntConst.ZERO);
            derivation.setLastMergeTime(new java.util.Date());
            derivationMapper.updateById(derivation);
        }
        return BeanUtil.copy(version, VersionVO.class);
    }

    @Override
    public List<TemplateVariableVO> variables(Long versionId) {
        return varMapper.selectList(Wrappers.<PipelineTemplateVar>lambdaQuery()
                        .eq(PipelineTemplateVar::getTemplateVersionId, versionId)
                        .orderByAsc(PipelineTemplateVar::getName))
                .stream().map(v -> BeanUtil.copy(v, TemplateVariableVO.class)).collect(Collectors.toList());
    }

    /**
     * 逐条合并：items 每项含 path（点分字段路径）、op（set/unset）、value。
     */
    @SuppressWarnings("unchecked")
    private String applyMergeItems(String baseJson, List<Map<String, Object>> items) {
        tools.jackson.databind.json.JsonMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        tools.jackson.databind.JsonNode root;
        try {
            root = mapper.readTree(baseJson);
            for (Map<String, Object> item : items) {
                String path = (String) item.get("path");
                String op = (String) item.get("op");
                Object value = item.get("value");
                if (StrUtil.isBlank(path)) {
                    continue;
                }
                String[] segments = path.split("\\.");
                tools.jackson.databind.JsonNode node = root;
                for (int i = 0; i < segments.length - 1; i++) {
                    node = node.path(segments[i]);
                }
                String leaf = segments[segments.length - 1];
                if ("unset".equals(op) && node.isObject()) {
                    ((tools.jackson.databind.node.ObjectNode) node).remove(leaf);
                } else if (node.isObject()) {
                    ((tools.jackson.databind.node.ObjectNode) node).set(leaf, mapper.valueToTree(value));
                }
            }
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new ServiceException("合并项格式错误");
        }
    }

    private void checkVariablesDeclared(String contentJson, List<TemplateVariableDTO> variables) {
        if (CollUtil.isEmpty(variables) || StrUtil.isBlank(contentJson)) {
            return;
        }
        java.util.Set<String> declared = variables.stream().map(TemplateVariableDTO::getName).collect(Collectors.toSet());
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\$\\{([a-zA-Z0-9_]+)\\}").matcher(contentJson);
        while (matcher.find()) {
            if (!declared.contains(matcher.group(1))) {
                throw new ServiceException("占位变量未声明：" + matcher.group(1));
            }
        }
    }

    private String currentJson(PipelineTemplate template) {
        if (ObjUtil.isEmpty(template.getCurrentVersionId())) {
            return "{\"schemaVersion\":1,\"stages\":[]}";
        }
        PipelineTemplateVersion version = versionMapper.selectById(template.getCurrentVersionId());
        return version == null ? "{\"schemaVersion\":1,\"stages\":[]}" : version.getContentJson();
    }

    private String currentPipelineJson(Pipeline pipeline) {
        if (ObjUtil.isEmpty(pipeline.getCurrentVersionId())) {
            return "{\"schemaVersion\":1,\"stages\":[]}";
        }
        PipelineVersion version = pipelineVersionMapper.selectById(pipeline.getCurrentVersionId());
        return version == null ? "{\"schemaVersion\":1,\"stages\":[]}" : version.getContentJson();
    }

    private PipelineTemplateVersion createVersion(Long templateId, String contentJson, Integer sourceType,
                                                  String changeNote, Long sourceVersionId) {
        String hash = CryptoUtil.sha256Hex(contentJson);
        int versionNo = versionMapper.selectMaxVersionNo(templateId) + 1;
        PipelineTemplateVersion version = new PipelineTemplateVersion();
        version.setTemplateId(templateId);
        version.setVersionNo(versionNo);
        version.setContentJson(contentJson);
        version.setContentHash(hash);
        version.setChangeNote(StrUtil.isBlank(changeNote) ? "" : changeNote);
        version.setSourceVersionId(sourceVersionId);
        versionMapper.insert(version);
        return version;
    }

    private void saveVars(Long versionId, List<TemplateVariableDTO> variables) {
        varMapper.delete(Wrappers.<PipelineTemplateVar>lambdaQuery()
                .eq(PipelineTemplateVar::getTemplateVersionId, versionId));
        if (CollUtil.isEmpty(variables)) {
            return;
        }
        for (TemplateVariableDTO dto : variables) {
            PipelineTemplateVar var = new PipelineTemplateVar();
            var.setTemplateVersionId(versionId);
            var.setName(dto.getName());
            var.setDescription(dto.getDescription());
            var.setDefaultValue(dto.getDefaultValue());
            var.setIsRequired(dto.getIsRequired() == null ? IntConst.ZERO : dto.getIsRequired());
            varMapper.insert(var);
        }
    }

    private PipelineTemplate requireTemplate(Long id) {
        PipelineTemplate template = getById(id);
        if (template == null) {
            throw new ServiceException("模板不存在");
        }
        return template;
    }

    private List<DiffVO.DiffLine> lineDiff(String left, String right) {
        String[] leftLines = (left == null ? "" : left).split("\\R", -1);
        String[] rightLines = (right == null ? "" : right).split("\\R", -1);
        List<DiffVO.DiffLine> lines = new ArrayList<>();
        int max = Math.max(leftLines.length, rightLines.length);
        for (int i = 0; i < max; i++) {
            String l = i < leftLines.length ? leftLines[i] : null;
            String r = i < rightLines.length ? rightLines[i] : null;
            DiffVO.DiffLine line = new DiffVO.DiffLine();
            if (l != null && r != null && l.equals(r)) {
                line.setType("unchanged");
                line.setText(l);
            } else if (l == null) {
                line.setType("added");
                line.setRightNo(i + 1);
                line.setText(r);
            } else if (r == null) {
                line.setType("removed");
                line.setLeftNo(i + 1);
                line.setText(l);
            } else {
                line.setType("removed");
                line.setLeftNo(i + 1);
                line.setText(l);
                DiffVO.DiffLine added = new DiffVO.DiffLine();
                added.setType("added");
                added.setRightNo(i + 1);
                added.setText(r);
                lines.add(added);
            }
            lines.add(line);
        }
        return lines;
    }
}
