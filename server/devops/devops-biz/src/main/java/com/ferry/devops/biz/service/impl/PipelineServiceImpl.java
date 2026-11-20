package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
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
import com.ferry.devops.biz.mapper.RunTriggerMapper;
import com.ferry.devops.biz.service.IPipelineService;
import com.ferry.devops.biz.wrapper.PipelineWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流水线服务实现：定义落库到阶段/任务表，按版本号管理，提供校验、回退与 diff。
 */
@Service
@RequiredArgsConstructor
public class PipelineServiceImpl extends ServiceImpl<PipelineMapper, Pipeline> implements IPipelineService {
    private final RunTriggerMapper triggerMapper;
    private final PipelineDefinitionStore definitionStore;

    @Override
    public IPage<PipelineVO> pageList(PipelineQuery body) {
        PipelineWrapper wrapper = PipelineWrapper.build(baseMapper);
        IPage<Pipeline> pages = page(Page.create(body), wrapper.lambda(body));
        IPage<PipelineVO> result = wrapper.pageVo(pages);
        result.setRecords(wrapper.voWithExtras(pages.getRecords()));
        return result;
    }

    @Override
    public PipelineDetailVO detail(Long id) {
        Pipeline pipeline = requirePipeline(id);
        PipelineDetailVO vo = new PipelineDetailVO();
        vo.setPipeline(PipelineWrapper.build(baseMapper).vo(pipeline));
        if (pipeline.getVersion() != null && pipeline.getVersion() > 0) {
            vo.setDefinition(definition(id, null));
        }
        vo.setVersions(versionPage(id, 1L, 20L).getRecords());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PipelineDTO body) {
        Pipeline pipeline;
        boolean isNew = ObjUtil.isEmpty(body.getId());
        if (isNew) {
            pipeline = new Pipeline();
            pipeline.setStatus(1);
            pipeline.setVersion(0);
        } else {
            pipeline = requirePipeline(body.getId());
        }
        pipeline.setAppId(body.getAppId());
        pipeline.setName(body.getName());
        if (ObjUtil.isNotEmpty(body.getStatus())) {
            pipeline.setStatus(body.getStatus());
        }
        boolean success = saveOrUpdate(pipeline);
        // 新建时若携带定义，生成初版
        if (isNew) {
            String definition = body.getDefinition();
            if (StrUtil.isNotBlank(definition)) {
                createVersion(pipeline.getId(), definition, body.getChangeNote());
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
            if (baseMapper.countActiveRun(id) > 0) {
                throw new ServiceException("流水线存在进行中的运行，不可删除");
            }
        }
        for (Long id : ids) {
            definitionStore.deleteByPipeline(id);
        }
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enable(Long id) {
        Pipeline pipeline = requirePipeline(id);
        pipeline.setStatus(1);
        return updateById(pipeline);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(Long id) {
        Pipeline pipeline = requirePipeline(id);
        pipeline.setStatus(0);
        return updateById(pipeline);
    }

    @Override
    public PipelineDefinitionVO definition(Long id, Long versionId) {
        Pipeline pipeline = requirePipeline(id);
        int version = versionId == null ? (pipeline.getVersion() == null ? 0 : pipeline.getVersion()) : versionId.intValue();
        String json = definitionStore.loadJson(id, version);
        PipelineDefinitionVO vo = new PipelineDefinitionVO();
        vo.setVersionId((long) version);
        vo.setVersionNo(version);
        vo.setJson(json);
        PipelineDefinitionDTO dto = definitionStore.load(id, version);
        if (dto.getStages() != null) {
            vo.setStages(dto.getStages().stream().map(this::stageVo).collect(Collectors.toList()));
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
        int newVersion = (pipeline.getVersion() == null ? 0 : pipeline.getVersion()) + 1;
        createVersion(id, definition, changeNote);
        VersionVO vo = new VersionVO();
        vo.setPipelineId(id);
        vo.setVersionNo(newVersion);
        vo.setChangeNote(changeNote);
        return vo;
    }

    @Override
    public IPage<VersionVO> versionPage(Long pipelineId, Long current, Long size) {
        List<Integer> versions = definitionStore.listVersions(pipelineId);
        long total = versions.size();
        long cur = current == null ? 1 : current;
        long sz = size == null ? 20 : size;
        int from = (int) Math.min((cur - 1) * sz, total);
        int to = (int) Math.min(from + sz, total);
        List<VersionVO> records = versions.subList(from, to).stream().map(v -> {
            VersionVO vo = new VersionVO();
            vo.setPipelineId(pipelineId);
            vo.setVersionNo(v);
            return vo;
        }).collect(Collectors.toList());
        IPage<VersionVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(cur, sz, total);
        result.setRecords(records);
        return result;
    }

    @Override
    public DiffVO versionDiff(Long leftId, Long rightId) {
        Pipeline pipeline = requirePipeline(leftId);
        String left = definitionStore.loadJson(pipeline.getId(), leftId.intValue());
        String right = definitionStore.loadJson(pipeline.getId(), rightId.intValue());
        DiffVO vo = new DiffVO();
        vo.setLeft(left);
        vo.setRight(right);
        vo.setLines(lineDiff(left, right));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VersionVO rollback(Long id, Long versionId, String changeNote) {
        Pipeline pipeline = requirePipeline(id);
        int target = versionId.intValue();
        if (!definitionStore.listVersions(id).contains(target)) {
            throw new ServiceException("回退版本不存在");
        }
        int newVersion = (pipeline.getVersion() == null ? 0 : pipeline.getVersion()) + 1;
        String json = definitionStore.loadJson(id, target);
        createVersion(id, json, changeNote);
        VersionVO vo = new VersionVO();
        vo.setPipelineId(id);
        vo.setVersionNo(newVersion);
        vo.setChangeNote(changeNote);
        return vo;
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
     * 创建新版本：阶段/任务落库并更新流水线当前版本号。
     */
    private void createVersion(Long pipelineId, String definitionJson, String changeNote) {
        PipelineDefinitionDTO dto;
        try {
            dto = JsonUtil.toBean(definitionJson, PipelineDefinitionDTO.class);
        } catch (Exception e) {
            throw new ServiceException("流水线定义不是合法的 JSON");
        }
        if (dto == null) {
            throw new ServiceException("流水线定义格式错误");
        }
        Pipeline pipeline = getById(pipelineId);
        int version = (pipeline.getVersion() == null ? 0 : pipeline.getVersion()) + 1;
        definitionStore.save(pipelineId, version, dto);
        pipeline.setVersion(version);
        updateById(pipeline);
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
