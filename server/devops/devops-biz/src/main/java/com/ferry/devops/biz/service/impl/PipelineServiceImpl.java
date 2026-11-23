package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
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
import com.ferry.devops.api.enums.PipelineStatusEnum;
import com.ferry.devops.api.vo.PipelineStageVO;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.api.vo.VersionVO;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.service.IAppService;
import com.ferry.devops.biz.service.IPipelineService;
import com.ferry.devops.biz.service.IPipelineStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流水线服务实现：定义落库到阶段/任务三表，并编排 Jenkins Job 同步。
 *
 * <p>{@link #save} 在同一事务内完成定义落库与 Jenkins Job 同步；画布定义保存/回退同步执行并返回同步结果。
 * 数据库与 Jenkins 不宣称原子事务：同步失败时定义已落库（状态落为同步失败），可重试同步；
 * 触发侧凭状态拦截未同步脚本。画布定义的版本变更检测用库内定义与提交定义的规范化 JSON 比对
 * （无摘要列）；版本递增在事务模板内以 CAS 方式完成，配合分段锁串行化同流水线的并发保存
 * （内存锁仅单实例有效，多实例部署依赖 CAS 冲突报错兜底）。
 */
@Service
@RequiredArgsConstructor
public class PipelineServiceImpl extends ServiceImpl<PipelineMapper, Pipeline> implements IPipelineService {
    private final IAppService appService;
    private final PipelineJobSyncService jobSyncService;
    private final IPipelineStageService pipelineStageService;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PipelineDTO body) {
        Pipeline entity = BeanUtil.copy(body, Pipeline.class);
        if (ObjUtil.isEmpty(body.getId())) {
            long count = count(Wrappers.<Pipeline>lambdaQuery().eq(Pipeline::getAppId, body.getAppId()));
            if (count > IntConst.ZERO) {
                throw new ServiceException("该应用已存在流水线");
            }
            entity.setVersion(IntConst.ZERO);
        } else {
            Pipeline pipeline = getById(body.getId());
            if (pipeline == null) {
                throw new ServiceException("流水线不存在");
            }
            entity.setVersion(pipeline.getVersion() + IntConst.ONE);
        }
        boolean isSuccess = saveOrUpdate(entity);
        if (isSuccess) {
            if (CollUtil.isNotEmpty(body.getStages())) {
                List<PipelineStageDTO> stages = body.getStages();
                int stageSort = IntConst.ONE;
                for (PipelineStageDTO stage : stages) {
                    stage.setPipelineId(entity.getId());
                    stage.setVersion(entity.getVersion());
                    stage.setSort(stageSort++);
                }
                pipelineStageService.save(stages);
            }
            jobSyncService.sync(entity.getId());
        }
        return isSuccess;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException("请选择要删除的流水线");
        }
        if (baseMapper.countActiveRun(ids) > IntConst.ZERO) {
            throw new ServiceException("流水线存在进行中的运行，不可删除");
        }
        boolean isSuccess = removeByIds(ids);
        if (isSuccess) {
            pipelineStageService.deleted(ids);
        }
        return isSuccess;
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
     * 版本回退：复制旧版本内容为新当前版本（版本号推进、内容回退）并同步 Jenkins。
     *
     * <p>旧版本运行断点重跑也经由该方法先回退到运行时版本，避免新脚本跑旧阶段快照。
     */
    @Override
    public VersionVO rollback(Long id, Long versionId, String changeNote) {
        Pipeline pipeline = getById(id);
        if (pipeline == null) {
            throw new ServiceException("流水线不存在");
        }
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
}
