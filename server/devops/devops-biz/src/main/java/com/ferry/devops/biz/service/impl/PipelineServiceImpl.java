package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.RunTrigger;
import com.ferry.devops.api.enums.PipelineStatusEnum;
import com.ferry.devops.api.vo.DiffVO;
import com.ferry.devops.api.vo.PipelineDefinitionVO;
import com.ferry.devops.api.vo.PipelinePreviewVO;
import com.ferry.devops.api.vo.PipelineStageVO;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.api.vo.TriggerRecordVO;
import com.ferry.devops.api.vo.ValidateResultVO;
import com.ferry.devops.api.vo.VersionVO;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.mapper.RunTriggerMapper;
import com.ferry.devops.biz.service.IAppService;
import com.ferry.devops.biz.service.IPipelineService;
import com.ferry.devops.biz.service.IPipelineStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 流水线服务实现：定义落库到阶段/任务三表，保存后经由 {@link PipelineJobSyncService} 同步 Jenkins Job。
 *
 * <p>保存/回退/启停共用同一同步编排，数据库与 Jenkins 不宣称原子事务：
 * 同步失败时定义已落库（状态落为同步失败），可重试同步；触发侧凭状态拦截未同步脚本。
 * 版本变更检测用库内定义与提交定义的规范化 JSON 比对（无摘要列）；
 * 版本递增在事务模板内以 CAS 方式完成，配合分段锁串行化同流水线的并发保存
 * （内存锁仅单实例有效，多实例部署依赖 CAS 冲突报错兜底）。
 */
@Service
@RequiredArgsConstructor
public class PipelineServiceImpl extends ServiceImpl<PipelineMapper, Pipeline> implements IPipelineService {
    private final IAppService appService;
    private final PipelineJobSyncService jobSyncService;
    private final IPipelineStageService pipelineStageService;

    private final RunTriggerMapper triggerMapper;
    private final TransactionTemplate transactionTemplate;

    /** 同流水线版本递增的分段锁（单实例内存锁） */
    private final ConcurrentHashMap<Long, Object> versionLocks = new ConcurrentHashMap<>();

    @Override
    public IPage<PipelineVO> pageList(PipelineQuery body) {
        return baseMapper.selectPageList(Page.create(body), body);
    }

    @Override
    public PipelineVO detail(Long id) {
        Pipeline pipeline = getById(id);
        if (pipeline == null) {
            throw new ServiceException("流水线不存在");
        }
        PipelineVO vo = BeanUtil.copy(pipeline, PipelineVO.class);
        App app = appService.getById(pipeline.getAppId());
        if (app != null) {
            vo.setAppName(app.getName());
            vo.setRepoBranch(app.getRepoBranch());
        }
        PipelineStageDTO stageDTO = new PipelineStageDTO();
        stageDTO.setPipelineId(pipeline.getId());
        stageDTO.setVersion(pipeline.getVersion());
        List<PipelineStageVO> stageVOList = pipelineStageService.list(stageDTO);
        if (CollUtil.isNotEmpty(stageVOList)) {
            vo.setStages(stageVOList);
            vo.setStageCount(stageVOList.size());
        }
        return vo;
    }

    /**
     * 新增或编辑：元数据与阶段定义单一保存入口，保存后同步 Jenkins Job。
     *
     * <p>同步失败不回滚保存（配置已保存，状态落为同步失败），响应文案提示可重试。
     */
    @Override
    public String save(PipelineDTO body) {
        boolean isNew = ObjUtil.isEmpty(body.getId());
        if (isNew) {
            // 应用与流水线 1:1：Jenkins Job 以应用编码命名，多流水线会互相覆盖脚本
            long exists = count(Wrappers.<Pipeline>lambdaQuery().eq(Pipeline::getAppId, body.getAppId()));
            if (exists > 0) {
                throw new ServiceException("该应用已存在流水线，请直接编辑");
            }
        }
        Pipeline pipeline = isNew ? new Pipeline() : requirePipeline(body.getId());
        pipeline.setAppId(body.getAppId());
        pipeline.setName(body.getName());
        if (isNew) {
            pipeline.setStatus(body.getStatus() == null
                    ? PipelineStatusEnum.ENABLED.getCode() : body.getStatus());
            pipeline.setVersion(0);
        } else if (ObjUtil.isNotEmpty(body.getStatus())) {
            pipeline.setStatus(body.getStatus());
        }
        saveOrUpdate(pipeline);
        // 定义变更：显式提交 stages（空数组=清空旧脚本）；未提交（null）视为不修改定义
        if (body.getStages() != null) {
            persistDefinition(pipeline, body.getStages());
        }
        PipelineJobSyncService.SyncOutcome outcome = jobSyncService.sync(pipeline.getId());
        return composeMessage(isNew ? "新增成功" : "修改成功", outcome);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的流水线");
        }
        for (Long id : ids) {
            if (baseMapper.countActiveRun(id) > 0) {
                throw new ServiceException("流水线存在进行中的运行，不可删除");
            }
        }
        for (Long id : ids) {
            pipelineStageService.deleteByPipeline(id);
        }
        removeByIds(ids);
        return "删除成功";
    }

    @Override
    public boolean changeStatus(Long id, Integer status) {
        if (!PipelineStatusEnum.ENABLED.getCode().equals(status)
                && !PipelineStatusEnum.DISABLED.getCode().equals(status)) {
            throw new ServiceException("流水线状态仅支持启用或停用");
        }
        return update(Wrappers.<Pipeline>lambdaUpdate()
                .set(Pipeline::getStatus, status).eq(Pipeline::getId, id));
    }

    /**
     * Jenkins Job 同步重试入口：对同步失败（或停用待补齐禁用位）的流水线重新执行同步编排。
     */
    @Override
    public String sync(Long id) {
        requirePipeline(id);
        PipelineJobSyncService.SyncOutcome outcome = jobSyncService.sync(id);
        return outcome.synced() ? "同步成功" : "同步失败：" + outcome.message();
    }

    @Override
    public PipelineDefinitionVO definition(Long id, Long versionId) {
        Pipeline pipeline = requirePipeline(id);
        int version = versionId == null ? (pipeline.getVersion() == null ? 0 : pipeline.getVersion()) : versionId.intValue();
        String json = pipelineStageService.loadDefinitionJson(id, version);
        PipelineDefinitionVO vo = new PipelineDefinitionVO();
        vo.setVersionNo(version);
        vo.setJson(json);
        PipelineStageDTO stageDTO = new PipelineStageDTO();
        stageDTO.setPipelineId(id);
        stageDTO.setVersion(version);
        vo.setStages(pipelineStageService.list(stageDTO));
        return vo;
    }

    @Override
    public ValidateResultVO validateDefinition(String definition, Integer mode) {
        return PipelineDefinitionCodec.validate(definition, mode);
    }

    @Override
    public PipelinePreviewVO previewDefinition(String definition, Integer mode) {
        if (Integer.valueOf(2).equals(mode)) {
            throw new ServiceException("YAML 模式暂不支持脚本预览，请切换到表单模式");
        }
        return PipelineScriptRenderer.preview(PipelineDefinitionCodec.decode(definition));
    }

    /**
     * 保存定义生成新版本（编辑器定义保存）：校验 → 版本落库（内容未变更不建版本）→ 同步。
     */
    @Override
    public VersionVO saveDefinition(Long id, String definition, Integer mode, String changeNote) {
        Pipeline pipeline = requirePipeline(id);
        PipelineDefinitionDTO dto = PipelineDefinitionCodec.decode(definition);
        ValidateResultVO result = PipelineDefinitionCodec.validateDto(dto, false);
        if (!Boolean.TRUE.equals(result.getValid())) {
            throw new ServiceException("流水线定义校验不通过：" + result.getErrors().get(0).getMessage());
        }
        boolean changed = persistDefinition(pipeline, dto.getStages());
        PipelineJobSyncService.SyncOutcome outcome = null;
        // 内容变更必须同步；未变更时若不是已同步启用态（失败重试/待配置补齐）也执行同步
        if (changed || !PipelineStatusEnum.ENABLED.getCode().equals(pipeline.getStatus())) {
            outcome = jobSyncService.sync(id);
        }
        return versionVo(id, changeNote, outcome, pipeline.getStatus());
    }

    @Override
    public IPage<VersionVO> versionPage(Long pipelineId, Long current, Long size) {
        List<Integer> versions = pipelineStageService.listVersions(pipelineId);
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
        String left = pipelineStageService.loadDefinitionJson(pipeline.getId(), leftId.intValue());
        String right = pipelineStageService.loadDefinitionJson(pipeline.getId(), rightId.intValue());
        DiffVO vo = new DiffVO();
        vo.setLeft(left);
        vo.setRight(right);
        vo.setLines(lineDiff(left, right));
        return vo;
    }

    /**
     * 版本回退：复制旧版本内容为新当前版本（版本号推进、内容回退）并同步 Jenkins。
     *
     * <p>旧版本运行断点重跑也经由该方法先回退到运行时版本，避免新脚本跑旧阶段快照。
     */
    @Override
    public VersionVO rollback(Long id, Long versionId, String changeNote) {
        Pipeline pipeline = requirePipeline(id);
        int target = versionId.intValue();
        if (!pipelineStageService.listVersions(id).contains(target)) {
            throw new ServiceException("回退版本不存在");
        }
        PipelineDefinitionDTO dto = JsonUtil.toBean(
                pipelineStageService.loadDefinitionJson(id, target), PipelineDefinitionDTO.class);
        if (dto == null) {
            throw new ServiceException("回退版本内容缺失，无法回退");
        }
        boolean changed = persistDefinition(pipeline, dto.getStages() == null ? List.of() : dto.getStages());
        PipelineJobSyncService.SyncOutcome outcome = changed ? jobSyncService.sync(id) : null;
        return versionVo(id, changeNote, outcome, pipeline.getStatus());
    }

    @Override
    public IPage<TriggerRecordVO> triggerRecords(Long appId, Long current, Long size) {
        List<RunTrigger> records = triggerMapper.selectRecentByApp(appId);
        IPage<TriggerRecordVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                current == null ? 1 : current, size == null ? 20 : size, records.size());
        result.setRecords(records.stream().map(t -> BeanUtil.copy(t, TriggerRecordVO.class)).collect(Collectors.toList()));
        return result;
    }

    // ==================== 内部实现 ====================

    /**
     * 定义落库（含变更检测）：与库内当前定义做规范化 JSON 比对，内容未变更不建新版本；
     * 空列表 = 显式清空（建空版本，同步时生成空脚本并禁用 Job）。
     *
     * @return 定义是否发生变化（产生新版本）
     */
    private boolean persistDefinition(Pipeline pipeline, List<PipelineStageDTO> stages) {
        Long pipelineId = pipeline.getId();
        PipelineDefinitionDTO incoming = new PipelineDefinitionDTO();
        incoming.setStages(stages);
        Integer currentVersion = pipeline.getVersion();
        if (currentVersion == null || currentVersion == 0) {
            if (stages.isEmpty()) {
                return false;
            }
            createVersion(pipelineId, incoming);
            return true;
        }
        PipelineDefinitionDTO current = pipelineStageService.loadDefinition(pipelineId, currentVersion);
        if (ObjUtil.equals(JsonUtil.toJson(current), JsonUtil.toJson(incoming))) {
            return false;
        }
        createVersion(pipelineId, incoming);
        return true;
    }

    /**
     * 创建新版本：版本号在事务内 CAS 自增后，把结构化定义写入阶段/任务表，失败一起回滚。
     */
    private void createVersion(Long pipelineId, PipelineDefinitionDTO dto) {
        Object lock = versionLocks.computeIfAbsent(pipelineId, key -> new Object());
        synchronized (lock) {
            transactionTemplate.executeWithoutResult(tx -> {
                Pipeline pipeline = getById(pipelineId);
                int current = pipeline.getVersion() == null ? 0 : pipeline.getVersion();
                boolean updated = update(Wrappers.<Pipeline>lambdaUpdate()
                        .eq(Pipeline::getId, pipelineId)
                        .eq(Pipeline::getVersion, current)
                        .set(Pipeline::getVersion, current + 1));
                if (!updated) {
                    throw new ServiceException("流水线版本并发更新冲突，请重试保存");
                }
                pipelineStageService.saveDefinition(pipelineId, current + 1, dto);
            });
        }
    }

    private VersionVO versionVo(Long id, String changeNote,
                                PipelineJobSyncService.SyncOutcome outcome, Integer fallbackStatus) {
        Pipeline latest = getById(id);
        VersionVO vo = new VersionVO();
        vo.setPipelineId(id);
        vo.setVersionNo(latest.getVersion());
        vo.setChangeNote(changeNote);
        vo.setStatus(outcome == null ? fallbackStatus : outcome.status());
        return vo;
    }

    private String composeMessage(String base, PipelineJobSyncService.SyncOutcome outcome) {
        if (outcome.synced()) {
            return base;
        }
        return base + "，但 Jenkins 同步失败：" + outcome.message() + "，可稍后重试同步";
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
