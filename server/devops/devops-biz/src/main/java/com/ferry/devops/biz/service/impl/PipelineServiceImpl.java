package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.CryptoUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineTaskDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.PipelineTemplateDerivation;
import com.ferry.devops.api.entity.PipelineTemplateVersion;
import com.ferry.devops.api.entity.PipelineVersion;
import com.ferry.devops.api.entity.RunTrigger;
import com.ferry.devops.api.vo.DiffVO;
import com.ferry.devops.api.vo.PipelineDefinitionVO;
import com.ferry.devops.api.vo.PipelineDetailVO;
import com.ferry.devops.api.vo.PipelineStageVO;
import com.ferry.devops.api.vo.PipelineTaskVO;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.api.vo.TriggerRecordVO;
import com.ferry.devops.api.vo.ValidateResultVO;
import com.ferry.devops.api.vo.VersionVO;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.mapper.PipelineTemplateDerivationMapper;
import com.ferry.devops.biz.mapper.PipelineTemplateVersionMapper;
import com.ferry.devops.biz.mapper.PipelineVersionMapper;
import com.ferry.devops.biz.mapper.RunTriggerMapper;
import com.ferry.devops.biz.service.IPipelineService;
import com.ferry.devops.biz.wrapper.PipelineWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流水线服务实现：不可变版本快照、回退复制、定义校验、diff。
 */
@Service
@RequiredArgsConstructor
public class PipelineServiceImpl extends ServiceImpl<PipelineMapper, Pipeline> implements IPipelineService {
    private final PipelineVersionMapper versionMapper;
    private final PipelineTemplateVersionMapper templateVersionMapper;
    private final PipelineTemplateDerivationMapper derivationMapper;
    private final RunTriggerMapper triggerMapper;

    @Override
    public IPage<PipelineVO> pageList(PipelineQuery body) {
        PipelineWrapper wrapper = PipelineWrapper.build(baseMapper, versionMapper, derivationMapper);
        IPage<Pipeline> pages = page(Page.create(body), wrapper.lambda(body));
        IPage<PipelineVO> result = wrapper.pageVo(pages);
        result.setRecords(wrapper.voWithExtras(pages.getRecords()));
        return result;
    }

    @Override
    public PipelineDetailVO detail(Long id) {
        Pipeline pipeline = requirePipeline(id);
        PipelineDetailVO vo = new PipelineDetailVO();
        vo.setPipeline(PipelineWrapper.build(baseMapper, versionMapper, derivationMapper).vo(pipeline));
        if (ObjUtil.isNotEmpty(pipeline.getCurrentVersionId())) {
            vo.setDefinition(definition(id, pipeline.getCurrentVersionId()));
        }
        vo.setVersions(versionPage(id, 1L, 20L).getRecords());
        PipelineTemplateDerivation derivation = derivationMapper.selectByPipeline(id);
        if (derivation != null) {
            vo.setDerivation(BeanUtil.copy(derivation, com.ferry.devops.api.vo.DerivationVO.class));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PipelineDTO body) {
        Pipeline pipeline;
        boolean isNew = ObjUtil.isEmpty(body.getId());
        if (isNew) {
            pipeline = new Pipeline();
            pipeline.setIsEnabled(IntConst.ONE);
        } else {
            pipeline = requirePipeline(body.getId());
        }
        pipeline.setAppId(body.getAppId());
        pipeline.setName(body.getName());
        if (ObjUtil.isNotEmpty(body.getOwnerId())) {
            pipeline.setOwnerId(body.getOwnerId());
        }
        if (ObjUtil.isNotEmpty(body.getIsEnabled())) {
            pipeline.setIsEnabled(body.getIsEnabled());
        }
        if (ObjUtil.isNotEmpty(body.getDisabledReason())) {
            pipeline.setDisabledReason(body.getDisabledReason());
        }
        boolean success = saveOrUpdate(pipeline);
        // 新建时若携带定义或派生模板，生成初版
        if (isNew) {
            String definition = body.getDefinition();
            if (StrUtil.isBlank(definition) && ObjUtil.isNotEmpty(body.getTemplateVersionId())) {
                definition = templateVersionMapper.selectById(body.getTemplateVersionId()).getContentJson();
            }
            if (StrUtil.isNotBlank(definition)) {
                createVersion(pipeline.getId(), definition, null,
                        ObjUtil.isNotEmpty(body.getTemplateVersionId()) ? 4 : 1,
                        body.getChangeNote(), null);
            }
            // 从模板派生：写派生关系
            if (ObjUtil.isNotEmpty(body.getTemplateId()) && ObjUtil.isNotEmpty(body.getTemplateVersionId())) {
                PipelineTemplateDerivation derivation = new PipelineTemplateDerivation();
                derivation.setTemplateId(body.getTemplateId());
                derivation.setTemplateVersionId(body.getTemplateVersionId());
                derivation.setPipelineId(pipeline.getId());
                derivation.setIsBehind(IntConst.ZERO);
                derivationMapper.insert(derivation);
            }
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的流水线");
        }
        for (Long id : ids) {
            if (baseMapper.countActiveRun(id) > IntConst.ZERO) {
                throw new ServiceException("流水线存在进行中的运行，不可删除");
            }
        }
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enable(Long id) {
        Pipeline pipeline = requirePipeline(id);
        pipeline.setIsEnabled(IntConst.ONE);
        pipeline.setDisabledReason(null);
        return updateById(pipeline);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(Long id, String reason) {
        Pipeline pipeline = requirePipeline(id);
        pipeline.setIsEnabled(IntConst.ZERO);
        pipeline.setDisabledReason(reason);
        return updateById(pipeline);
    }

    @Override
    public PipelineDefinitionVO definition(Long id, Long versionId) {
        PipelineVersion version;
        if (ObjUtil.isNotEmpty(versionId)) {
            version = versionMapper.selectById(versionId);
        } else {
            Pipeline pipeline = requirePipeline(id);
            version = versionMapper.selectById(pipeline.getCurrentVersionId());
        }
        if (version == null) {
            throw new ServiceException("流水线版本不存在");
        }
        PipelineDefinitionVO vo = new PipelineDefinitionVO();
        vo.setVersionId(version.getId());
        vo.setVersionNo(version.getVersionNo());
        vo.setJson(version.getContentJson());
        vo.setYaml(version.getContentYaml());
        if (StrUtil.isNotBlank(version.getContentJson())) {
            try {
                PipelineDefinitionDTO dto = JsonUtil.toBean(version.getContentJson(), PipelineDefinitionDTO.class);
                if (dto != null && dto.getStages() != null) {
                    vo.setStages(dto.getStages().stream().map(this::stageVo).collect(Collectors.toList()));
                }
            } catch (Exception ignored) {
                // YAML 降级模式下 content_json 可能不是结构化 JSON，忽略阶段解析
            }
        }
        return vo;
    }

    @Override
    public ValidateResultVO validateDefinition(String definition, Integer mode) {
        return PipelineDefinitionCodec.validate(definition, mode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VersionVO saveDefinition(Long id, String definition, Integer mode, String changeNote) {
        Pipeline pipeline = requirePipeline(id);
        ValidateResultVO result = PipelineDefinitionCodec.validate(definition, mode);
        if (!Boolean.TRUE.equals(result.getValid())) {
            throw new ServiceException("流水线定义校验不通过：" + result.getErrors().get(0).getMessage());
        }
        String contentJson = normalize(definition, mode);
        String hash = CryptoUtil.sha256Hex(contentJson);
        PipelineVersion current = versionMapper.selectById(pipeline.getCurrentVersionId());
        if (current != null && hash.equals(current.getContentHash())) {
            throw new ServiceException("内容未变更");
        }
        PipelineVersion version = createVersion(id, contentJson, mode == null ? Integer.valueOf(1) : mode, changeNote, null);
        return BeanUtil.copy(version, VersionVO.class);
    }

    @Override
    public IPage<VersionVO> versionPage(Long pipelineId, Long current, Long size) {
        IPage<PipelineVersion> pages = versionMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        current == null ? 1 : current, size == null ? 20 : size),
                Wrappers.<PipelineVersion>lambdaQuery()
                        .eq(PipelineVersion::getPipelineId, pipelineId)
                        .orderByDesc(PipelineVersion::getVersionNo));
        IPage<VersionVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        result.setRecords(pages.getRecords().stream().map(v -> BeanUtil.copy(v, VersionVO.class)).collect(Collectors.toList()));
        return result;
    }

    @Override
    public DiffVO versionDiff(Long leftId, Long rightId) {
        PipelineVersion left = versionMapper.selectById(leftId);
        PipelineVersion right = versionMapper.selectById(rightId);
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
    public VersionVO rollback(Long id, Long versionId, String changeNote) {
        PipelineVersion target = versionMapper.selectById(versionId);
        if (target == null || !ObjUtil.equals(target.getPipelineId(), id)) {
            throw new ServiceException("回退版本不存在");
        }
        PipelineVersion version = createVersion(id, target.getContentJson(), 3, changeNote, target.getId());
        return BeanUtil.copy(version, VersionVO.class);
    }

    @Override
    public IPage<TriggerRecordVO> triggerRecords(Long appId, Long current, Long size) {
        List<RunTrigger> records = triggerMapper.selectRecentByApp(appId);
        IPage<TriggerRecordVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                current == null ? 1 : current, size == null ? 20 : size, records.size());
        result.setRecords(records.stream().map(t -> BeanUtil.copy(t, TriggerRecordVO.class)).collect(Collectors.toList()));
        return result;
    }

    /**
     * 创建不可变版本快照，并更新流水线当前版本指针。
     * source_type 由调用方按入口明确传入：1表单 2YAML 3回退 4模板合并。
     */
    private PipelineVersion createVersion(Long pipelineId, String contentJson, Integer sourceType,
                                          String changeNote, Long sourceVersionId) {
        return createVersion(pipelineId, contentJson, null, sourceType, changeNote, sourceVersionId);
    }

    private PipelineVersion createVersion(Long pipelineId, String contentJson, String contentYaml,
                                          Integer sourceType, String changeNote, Long sourceVersionId) {
        String hash = CryptoUtil.sha256Hex(contentJson);
        // 同内容去重：已有相同 hash 不再新增版本，直接复用
        Long existing = versionMapper.selectIdByHash(pipelineId, hash);
        if (existing != null) {
            Pipeline pipeline = getById(pipelineId);
            pipeline.setCurrentVersionId(existing);
            updateById(pipeline);
            return versionMapper.selectById(existing);
        }
        int versionNo = versionMapper.selectMaxVersionNo(pipelineId) + 1;
        PipelineVersion version = new PipelineVersion();
        version.setPipelineId(pipelineId);
        version.setVersionNo(versionNo);
        version.setContentJson(contentJson);
        version.setContentYaml(contentYaml);
        version.setContentHash(hash);
        version.setChangeNote(StrUtil.isBlank(changeNote) ? "" : changeNote);
        version.setSourceType(sourceType == null ? 1 : sourceType);
        version.setSourceVersionId(sourceVersionId);
        versionMapper.insert(version);
        Pipeline pipeline = getById(pipelineId);
        pipeline.setCurrentVersionId(version.getId());
        updateById(pipeline);
        return version;
    }

    /**
     * YAML 模式降级：原样保存 YAML 文本到 content_json（JSON only，不解析），保持编译通过。
     */
    private String normalize(String definition, Integer mode) {
        return definition;
    }

    private PipelineStageVO stageVo(PipelineStageDTO stage) {
        PipelineStageVO vo = new PipelineStageVO();
        vo.setType(stage.getType());
        vo.setName(stage.getName());
        vo.setTimeoutSeconds(stage.getTimeoutSeconds());
        vo.setOutputs(stage.getOutputs());
        if (stage.getTasks() != null) {
            List<PipelineTaskVO> tasks = new ArrayList<>();
            for (PipelineTaskDTO task : stage.getTasks()) {
                PipelineTaskVO taskVo = new PipelineTaskVO();
                taskVo.setName(task.getName());
                taskVo.setExecutor(task.getExecutor());
                taskVo.setScriptRef(task.getScriptRef());
                taskVo.setParams(task.getParams());
                tasks.add(taskVo);
            }
            vo.setTasks(tasks);
        }
        return vo;
    }

    /**
     * 简单行级 diff：按行比较，标注新增/删除/未变。
     */
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

    private Pipeline requirePipeline(Long id) {
        Pipeline pipeline = getById(id);
        if (pipeline == null) {
            throw new ServiceException("流水线不存在");
        }
        return pipeline;
    }
}
