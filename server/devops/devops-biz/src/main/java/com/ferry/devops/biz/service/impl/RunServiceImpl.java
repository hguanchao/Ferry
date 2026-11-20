package com.ferry.devops.biz.service.impl;
import com.ferry.devops.api.vo.RunVO;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CryptoUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineTaskDTO;
import com.ferry.devops.api.dto.RunTriggerDTO;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunIdempotency;
import com.ferry.devops.api.entity.RunLogSegment;
import com.ferry.devops.api.entity.RunOperation;
import com.ferry.devops.api.entity.RunPrecheck;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.entity.RunTask;
import com.ferry.devops.api.entity.RunTrigger;
import com.ferry.devops.api.enums.BlockReasonEnum;
import com.ferry.devops.api.enums.DevopsStateMachine;
import com.ferry.devops.api.enums.PipelineStatusEnum;
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.api.enums.TriggerResultEnum;
import com.ferry.devops.api.vo.PrecheckItemVO;
import com.ferry.devops.api.vo.PrecheckResultVO;
import com.ferry.devops.api.vo.RunDetailVO;
import com.ferry.devops.api.vo.RunStageVO;
import com.ferry.devops.api.vo.RunTaskVO;
import com.ferry.devops.api.vo.TriggerResultVO;
import com.ferry.devops.api.vo.VersionVO;
import com.ferry.devops.biz.integration.jenkins.BuildSource;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
import com.ferry.devops.biz.integration.jenkins.JenkinsDispatchException;
import com.ferry.devops.biz.integration.jenkins.JenkinsDispatchResult;
import com.ferry.devops.biz.integration.jenkins.JenkinsJobNames;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.mapper.PipelineStageMapper;
import com.ferry.devops.biz.mapper.PipelineStageTaskMapper;
import com.ferry.devops.biz.mapper.RunIdempotencyMapper;
import com.ferry.devops.biz.mapper.RunLogSegmentMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunOperationMapper;
import com.ferry.devops.biz.mapper.RunPrecheckMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.mapper.RunTaskMapper;
import com.ferry.devops.biz.mapper.RunTriggerMapper;
import com.ferry.devops.biz.service.IPipelineService;
import com.ferry.devops.biz.service.IPipelineStageService;
import com.ferry.devops.biz.service.IRunService;
import com.ferry.devops.biz.wrapper.RunWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 运行服务实现：五项预校验、幂等触发、阶段编排、日志脱敏分段、断点重跑、运行干预。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunServiceImpl extends ServiceImpl<RunMapper, Run> implements IRunService {
    private static final Pattern COMMIT_PATTERN = Pattern.compile("^[0-9a-fA-F]{7,40}$");
    private static final Pattern REF_PATTERN = Pattern.compile("^[^\\s~^:?*\\[\\\\]{1,100}$");
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(password|passwd|token|secret|access[_-]?key|secret[_-]?key|api[_-]?key)\\s*[:=]\\s*\\S+");
    private static final int IDEMPotent_WINDOW_SECONDS = 60;
    private static final int MAX_LOG_LINES = 100_000;
    private static final int SEGMENT_LINES = 500;
    private static final int DEFAULT_LOG_LIMIT = 2000;

    private final RunMapper runMapper;
    private final PipelineMapper pipelineMapper;
    private final PipelineStageMapper pipelineStageMapper;
    private final PipelineStageTaskMapper pipelineTaskMapper;
    private final IPipelineStageService pipelineStageService;
    private final RunStageMapper stageMapper;
    private final RunTaskMapper taskMapper;
    private final RunLogSegmentMapper logMapper;
    private final RunPrecheckMapper precheckMapper;
    private final RunTriggerMapper triggerMapper;
    private final RunOperationMapper operationMapper;
    private final RunIdempotencyMapper idempotencyMapper;
    private final JenkinsClient jenkinsClient;
    private final AppMapper appMapper;
    private final IPipelineService pipelineService;

    /**
     * 触发生效的代码源。
     *
     * @param branch        生效分支
     * @param commitId      生效提交或引用值（未接入仓库解析前不做 SHA 归一）
     * @param refType       引用类型 1 Commit 2 Tag/Ref
     * @param branchFromApp 分支是否来自应用默认配置
     * @param commitFromApp 提交是否来自应用登记提交
     */
    private record ResolvedSource(String branch, String commitId, Integer refType,
                                  boolean branchFromApp, boolean commitFromApp) {
    }

    @Override
    public IPage<RunVO> pageList(RunQuery body) {
        RunWrapper wrapper = RunWrapper.build(pipelineMapper);
        IPage<Run> pages = page(Page.create(body), wrapper.lambda(body));
        IPage<RunVO> result = wrapper.pageVo(pages);
        result.setRecords(wrapper.voWithExtras(pages.getRecords()));
        return result;
    }

    @Override
    public RunDetailVO detail(Long id) {
        Run run = requireRun(id);
        RunWrapper wrapper = RunWrapper.build(pipelineMapper);
        RunDetailVO vo = new RunDetailVO();
        vo.setRun(wrapper.voWithExtras(Collections.singletonList(run)).get(0));
        List<RunStage> stages = stageMapper.selectByRun(id);
        List<RunTask> tasks = taskMapper.selectByRun(id);
        Map<Long, List<RunTask>> tasksByStage = tasks.stream()
                .collect(Collectors.groupingBy(RunTask::getStageId));
        List<RunStageVO> stageVos = stages.stream().map(s -> {
            RunStageVO sv = BeanUtil.copy(s, RunStageVO.class);
            sv.setTasks(tasksByStage.getOrDefault(s.getId(), Collections.emptyList()).stream()
                    .map(t -> BeanUtil.copy(t, RunTaskVO.class)).collect(Collectors.toList()));
            return sv;
        }).collect(Collectors.toList());
        vo.setStages(stageVos);
        vo.setTasks(tasks.stream().map(t -> BeanUtil.copy(t, RunTaskVO.class)).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public List<RunStageVO> stages(Long runId) {
        return stageMapper.selectByRun(runId).stream()
                .map(s -> BeanUtil.copy(s, RunStageVO.class)).collect(Collectors.toList());
    }

    @Override
    public List<RunTaskVO> tasks(Long stageId) {
        return taskMapper.selectByStage(stageId).stream()
                .map(t -> BeanUtil.copy(t, RunTaskVO.class)).collect(Collectors.toList());
    }

    @Override
    public List<?> log(Long taskId, Integer fromLine, Integer limit) {
        int from = fromLine == null ? 1 : fromLine;
        int size = limit == null ? DEFAULT_LOG_LIMIT : limit;
        List<RunLogSegment> segments = logMapper.selectByTask(taskId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (RunLogSegment seg : segments) {
            if (seg.getEndLine() < from) {
                continue;
            }
            if (result.size() >= size) {
                break;
            }
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", seg.getId());
            m.put("taskId", seg.getTaskId());
            m.put("segmentNo", seg.getSegmentNo());
            m.put("startLine", seg.getStartLine());
            m.put("endLine", seg.getEndLine());
            m.put("content", seg.getContent());
            m.put("isTruncated", seg.getIsTruncated());
            result.add(m);
        }
        return result;
    }

    @Override
    public List<?> logSearch(Long taskId, String keyword, Integer fromLine) {
        int from = fromLine == null ? 1 : fromLine;
        List<Map<String, Object>> hits = new ArrayList<>();
        for (RunLogSegment seg : logMapper.selectByTask(taskId)) {
            if (seg.getEndLine() < from || StrUtil.isBlank(seg.getContent())) {
                continue;
            }
            String[] lines = seg.getContent().split("\\R");
            int lineNo = seg.getStartLine();
            for (String line : lines) {
                if (lineNo >= from && line.contains(keyword)) {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("lineNo", lineNo);
                    m.put("content", line);
                    m.put("segmentNo", seg.getSegmentNo());
                    hits.add(m);
                }
                lineNo++;
            }
        }
        return hits;
    }

    @Override
    public void saveLog(Long runId, Long taskId, String rawLog) {
        if (rawLog == null) {
            return;
        }
        String[] lines = rawLog.split("\\R", -1);
        int total = Math.min(lines.length, MAX_LOG_LINES);
        boolean truncated = lines.length > MAX_LOG_LINES;
        // 按 500 行分段，段内保留原始行号，入库前脱敏
        for (int start = 0; start < total; start += SEGMENT_LINES) {
            int end = Math.min(start + SEGMENT_LINES, total);
            StringBuilder content = new StringBuilder();
            for (int i = start; i < end; i++) {
                content.append(mask(lines[i])).append('\n');
            }
            RunLogSegment segment = new RunLogSegment();
            segment.setRunId(runId);
            segment.setTaskId(taskId);
            segment.setSegmentNo(start / SEGMENT_LINES + 1);
            segment.setStartLine(start + 1);
            segment.setEndLine(end);
            segment.setContent(content.toString());
            segment.setIsTruncated(truncated && end == total ? 1 : 0);
            logMapper.insert(segment);
        }
        // 更新任务日志行数与截断标记
        RunTask task = taskMapper.selectById(taskId);
        if (task != null) {
            task.setLogLineCount(total);
            task.setIsTruncated(truncated ? 1 : 0);
            taskMapper.updateById(task);
        }
    }

    @Override
    public PrecheckResultVO precheck(RunTriggerDTO body) {
        App app = appMapper.selectById(body.getAppId());
        return doPrecheck(body, app, resolveSource(app, body));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TriggerResultVO trigger(RunTriggerDTO body) {
        // 频率管控：同一用户同一应用 60 秒内第 6 次触发拒绝
        if (runMapper.countByUserSince(body.getAppId(), AuthUtil.getUserId(),
                new Date(System.currentTimeMillis() - 60_000L)) >= 5) {
            throw new ServiceException("操作过于频繁，请稍后再试");
        }
        // 生效代码源只解析一次：预校验、运行落库、触发记录与幂等键共用，避免口径漂移
        App app = appMapper.selectById(body.getAppId());
        ResolvedSource source = resolveSource(app, body);
        PrecheckResultVO precheck = doPrecheck(body, app, source);
        if (!Boolean.TRUE.equals(precheck.getCanTrigger())) {
            writeTriggerRecord(body, source, 4, null, firstFailDetail(precheck));
            TriggerResultVO vo = new TriggerResultVO();
            vo.setResult(TriggerResultEnum.REJECTED.getCode());
            vo.setMessage("预校验不通过：" + firstFailDetail(precheck));
            return vo;
        }
        // 幂等命中
        if (ObjUtil.isNotEmpty(precheck.getMergedRunId())) {
            writeTriggerRecord(body, source, 2, precheck.getMergedRunId(), null);
            TriggerResultVO vo = new TriggerResultVO();
            vo.setResult(TriggerResultEnum.MERGED.getCode());
            vo.setRunId(precheck.getMergedRunId());
            vo.setMessage("命中幂等键，已合并到进行中的运行");
            return vo;
        }
        if (ObjUtil.isNotEmpty(precheck.getReusedRunId())) {
            writeTriggerRecord(body, source, 3, precheck.getReusedRunId(), null);
            TriggerResultVO vo = new TriggerResultVO();
            vo.setResult(TriggerResultEnum.REUSED.getCode());
            vo.setRunId(precheck.getReusedRunId());
            vo.setMessage("命中成功运行，将复用其制品");
            return vo;
        }
        // 幂等键
        Pipeline pipeline = pipelineMapper.selectById(body.getPipelineId());
        String idemKey = idemKey(body.getAppId(), source.commitId(), pipeline.getVersion());
        // 事务内创建运行 + 阶段 + 触发记录 + 幂等记录
        Run run = new Run();
        run.setRunNo(generateRunNo());
        run.setPipelineId(body.getPipelineId());
        run.setPipelineVersion(pipeline.getVersion());
        run.setAppId(body.getAppId());
        run.setBranch(source.branch());
        run.setCommitId(source.commitId());
        run.setRefType(source.refType());
        run.setRefValue(source.commitId());
        run.setTriggerType(IntConst.ONE);
        run.setTriggerUser(AuthUtil.getUserId());
        run.setTriggerNote(body.getNote());
        run.setStatus(RunStatusEnum.QUEUED.getCode());
        run.setIsPrecheckPass(IntConst.ONE);
        run.setIdempotentKey(idemKey);
        save(run);
        createStages(run.getId(), pipeline.getId(), pipeline.getVersion(), null);
        persistPrechecks(run.getId(), precheck.getItems());
        writeTriggerRecord(body, source, 1, run.getId(), null);
        RunIdempotency idem = new RunIdempotency();
        idem.setId(IdWorker.getId());
        idem.setIdempotentKey(idemKey);
        idem.setAppId(body.getAppId());
        idem.setRunId(run.getId());
        idem.setState(IntConst.ONE);
        idem.setExpireTime(new Date(System.currentTimeMillis() + IDEMPotent_WINDOW_SECONDS * 1000L));
        // 幂等键唯一：非活动运行结束后重复触发要刷新该行，而不是再次 INSERT
        idempotencyMapper.upsert(idem);
        // 事务提交后由 JobDispatchService 异步下发；最小实现记日志
        triggerJenkinsAfterCommit(run);
        TriggerResultVO vo = new TriggerResultVO();
        vo.setResult(TriggerResultEnum.CREATED.getCode());
        vo.setRunId(run.getId());
        vo.setMessage("运行已创建，状态排队中");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pause(Long id, String reason) {
        Run run = requireRun(id);
        if (!ObjUtil.equals(run.getStatus(), RunStatusEnum.RUNNING.getCode())
                && !ObjUtil.equals(run.getStatus(), RunStatusEnum.QUEUED.getCode())) {
            throw new ServiceException("仅运行中或排队中的运行可暂停");
        }
        // 排队中视同运行中进入阻塞边界，复用状态机 RUNNING→BLOCKED 校验
        DevopsStateMachine.checkRun(RunStatusEnum.RUNNING.getCode(), RunStatusEnum.BLOCKED.getCode());
        run.setStatus(RunStatusEnum.BLOCKED.getCode());
        run.setBlockReason(BlockReasonEnum.MANUAL_PAUSED.getCode());
        updateById(run);
        writeOperation(id, 1, reason);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resume(Long id) {
        Run run = requireRun(id);
        if (!ObjUtil.equals(run.getStatus(), RunStatusEnum.BLOCKED.getCode())
                || !ObjUtil.equals(run.getBlockReason(), BlockReasonEnum.MANUAL_PAUSED.getCode())) {
            throw new ServiceException("仅人工暂停的运行可继续");
        }
        // 人工暂停恢复视同排队中→运行中，复用状态机校验
        DevopsStateMachine.checkRun(RunStatusEnum.QUEUED.getCode(), RunStatusEnum.RUNNING.getCode());
        run.setStatus(RunStatusEnum.RUNNING.getCode());
        run.setBlockReason(null);
        updateById(run);
        writeOperation(id, 2, null);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean abort(Long id, String reason) {
        if (StrUtil.isBlank(reason) || reason.length() < 10) {
            throw new ServiceException("中止原因不少于 10 字");
        }
        Run run = requireRun(id);
        DevopsStateMachine.checkRun(run.getStatus(), RunStatusEnum.CANCELLED.getCode());
        run.setStatus(RunStatusEnum.CANCELLED.getCode());
        run.setEndTime(new Date());
        if (run.getStartTime() != null) {
            run.setDuration(run.getEndTime().getTime() - run.getStartTime().getTime());
        }
        updateById(run);
        writeOperation(id, 3, reason);
        if (run.getJenkinsBuildNo() != null) {
            String jobName = resolveRunJobName(run);
            if (jobName != null) {
                jenkinsClient.abortBuild(jobName, run.getJenkinsBuildNo());
            }
        }
        return true;
    }

    @Override
    public List<RunStageVO> rerunPlan(Long id) {
        Run run = requireRun(id);
        if (!ObjUtil.equals(run.getStatus(), RunStatusEnum.FAILURE.getCode())
                && !ObjUtil.equals(run.getStatus(), RunStatusEnum.TIMEOUT.getCode())) {
            throw new ServiceException("仅失败或超时的运行可断点重跑");
        }
        return stages(id).stream().map(s -> {
            RunStageVO vo = BeanUtil.copy(s, RunStageVO.class);
            // 成功且在失败阶段之前的阶段可复用
            boolean reusable = ObjUtil.equals(s.getStatus(), StageStatusEnum.SUCCESS.getCode());
            vo.setReusable(reusable);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TriggerResultVO rerun(RunTriggerDTO body) {
        Run original = requireRun(body.getId());
        if (!ObjUtil.equals(original.getStatus(), RunStatusEnum.FAILURE.getCode())
                && !ObjUtil.equals(original.getStatus(), RunStatusEnum.TIMEOUT.getCode())) {
            throw new ServiceException("仅失败或超时的运行可断点重跑");
        }
        List<RunStage> oldStages = stageMapper.selectByRun(original.getId());
        // 复用范围：fromStageId 之前且成功的阶段；未指定则从首个失败阶段算起
        int fromSeq = resolveFromSeq(oldStages, body.getFromStageId());
        // 旧版本重跑先回退生成当前版本：复制运行时定义为新版本并同步 Job，避免新脚本跑旧阶段快照
        Pipeline pipeline = pipelineMapper.selectById(original.getPipelineId());
        if (pipeline == null) {
            throw new ServiceException("流水线不存在，无法重跑");
        }
        if (original.getPipelineVersion() == null) {
            throw new ServiceException("原运行缺少版本信息，无法自动回退重跑");
        }
        if (!ObjUtil.equals(pipeline.getVersion(), original.getPipelineVersion())) {
            VersionVO rolled = pipelineService.rollback(original.getPipelineId(),
                    Long.valueOf(original.getPipelineVersion()), "断点重跑自动回退");
            if (!ObjUtil.equals(rolled.getStatus(), PipelineStatusEnum.ENABLED.getCode())) {
                throw new ServiceException("自动回退后 Jenkins 同步未成功，请稍后重试重跑");
            }
        }
        // 新建运行（不覆盖原运行），固定到回退后的当前版本
        Run run = new Run();
        run.setRunNo(generateRunNo());
        run.setPipelineId(original.getPipelineId());
        run.setPipelineVersion(pipelineMapper.selectById(original.getPipelineId()).getVersion());
        run.setAppId(original.getAppId());
        run.setBranch(original.getBranch());
        run.setCommitId(original.getCommitId());
        run.setRefType(original.getRefType());
        run.setRefValue(original.getRefValue());
        run.setTriggerType(IntConst.ONE);
        run.setTriggerUser(AuthUtil.getUserId());
        run.setTriggerNote(body.getNote());
        run.setStatus(RunStatusEnum.QUEUED.getCode());
        run.setIsPrecheckPass(IntConst.ONE);
        run.setIdempotentKey(original.getIdempotentKey() + "-R" + System.currentTimeMillis());
        run.setRerunFromRunId(original.getId());
        save(run);
        // 阶段：复用成功阶段（status=8 已复用，写 reused_from_stage_id），其余未开始
        for (RunStage old : oldStages) {
            RunStage stage = new RunStage();
            stage.setRunId(run.getId());
            stage.setStageType(old.getStageType());
            stage.setStageName(old.getStageName());
            stage.setSeq(old.getSeq());
            if (old.getSeq() < fromSeq && ObjUtil.equals(old.getStatus(), StageStatusEnum.SUCCESS.getCode())) {
                stage.setStatus(StageStatusEnum.REUSED.getCode());
                stage.setReusedFromStageId(old.getId());
                stage.setStartTime(old.getStartTime());
                stage.setEndTime(old.getEndTime());
                stage.setDuration(old.getDuration());
            } else {
                stage.setStatus(StageStatusEnum.NOT_STARTED.getCode());
            }
            stageMapper.insert(stage);
        }
        writeTriggerRecordFromRun(run, 1);
        triggerJenkinsAfterCommit(run);
        TriggerResultVO vo = new TriggerResultVO();
        vo.setResult(TriggerResultEnum.CREATED.getCode());
        vo.setRunId(run.getId());
        vo.setMessage("断点重跑已创建，前序成功阶段已复用");
        return vo;
    }

    // ==================== 内部实现 ====================

    /**
     * 五项预校验（仓库信息、分支、提交、定义匹配、幂等检查）。
     *
     * @param app    应用档案，可为 null（应用不存在时第 1 项直接不通过）
     * @param source 生效代码源：入参优先，未填时已回退应用登记的默认分支/提交
     */
    private PrecheckResultVO doPrecheck(RunTriggerDTO body, App app, ResolvedSource source) {
        Long appId = body.getAppId();
        Long pipelineId = body.getPipelineId();
        List<PrecheckItemVO> items = new ArrayList<>();

        // 1 仓库信息：校验应用管理登记的仓库信息完整
        PrecheckItemVO repoItem;
        if (app == null) {
            repoItem = item(1, false, "应用不存在");
        } else if (StrUtil.isBlank(app.getRepoUrl())) {
            repoItem = item(1, false, "应用未填写仓库地址");
        } else {
            repoItem = item(1, true, "仓库信息已登记：" + app.getRepoUrl());
        }
        items.add(repoItem);

        // 2 分支存在：触发时未指定则回退应用默认分支
        String branchDetail;
        if (StrUtil.isBlank(source.branch())) {
            branchDetail = "分支不能为空，且应用未登记默认分支";
        } else if (source.branchFromApp()) {
            branchDetail = "未指定分支，使用应用默认分支：" + source.branch();
        } else {
            branchDetail = "分支已指定：" + source.branch();
        }
        items.add(item(2, StrUtil.isNotBlank(source.branch()), branchDetail));

        // 3 提交可达：校验 7–40 位 hex 或合法 Git ref；未指定则回退应用登记的提交
        boolean commitOk = isRef(source.refType(), source.commitId());
        String commitDetail;
        if (StrUtil.isBlank(source.commitId())) {
            commitDetail = "提交不能为空，且应用未登记默认提交";
        } else if (commitOk && source.commitFromApp()) {
            commitDetail = "未指定提交，使用应用登记提交：" + source.commitId();
        } else if (commitOk) {
            commitDetail = "提交引用格式合法";
        } else if (source.commitFromApp()) {
            commitDetail = "应用登记的提交格式非法，需为 7–40 位十六进制";
        } else {
            commitDetail = "提交必须为 7–40 位十六进制或合法 Git ref";
        }
        items.add(item(3, commitOk, commitDetail));

        // 4 定义匹配：流水线启用（含已同步语义）+ 存在当前版本 + 定义中无空阶段/空任务/空命令
        Pipeline pipeline = pipelineMapper.selectById(pipelineId);
        PipelineStatusEnum pipelineStatus = pipeline == null ? null : PipelineStatusEnum.of(pipeline.getStatus());
        boolean defOk = pipeline != null && PipelineStatusEnum.ENABLED == pipelineStatus
                && ObjUtil.isNotEmpty(pipeline.getVersion());
        String defDetail;
        if (!defOk) {
            defDetail = switch (pipelineStatus == null ? PipelineStatusEnum.DISABLED : pipelineStatus) {
                case PENDING_CONFIG -> "流水线待配置：请先在画布完成定义并保存同步";
                case SYNC_FAILED -> "Jenkins 同步失败：请重试同步后再触发";
                case DISABLED -> "流水线已停用";
                case ENABLED -> "流水线无当前版本";
            };
        } else {
            // 保存阶段允许空任务，运行时才拦截：给出明确提示让用户在画布补齐后再触发
            defDetail = checkRunnableDefinition(pipeline.getId(), pipeline.getVersion());
            if (defDetail == null) {
                defDetail = "流水线已启用且脚本已同步";
            } else {
                defOk = false;
            }
        }
        items.add(item(4, defOk, defDetail));
        // 5 幂等检查：按 60 秒键查活动/成功运行
        String idemKey = idemKey(appId, source.commitId(),
                pipeline == null ? null : pipeline.getVersion());
        Run active = runMapper.selectActiveByIdemKey(idemKey);
        Run success = active != null ? null : runMapper.selectSuccessByIdemKey(idemKey);
        PrecheckItemVO idemItem = new PrecheckItemVO();
        idemItem.setType(5);
        if (active != null) {
            idemItem.setConclusion(3);
            idemItem.setDetail("命中幂等键，将合并到运行 " + active.getId());
        } else if (success != null) {
            idemItem.setConclusion(4);
            idemItem.setDetail("命中成功运行 " + success.getId());
        } else {
            idemItem.setConclusion(1);
            idemItem.setDetail("未命中幂等键，将创建新运行");
        }
        items.add(idemItem);

        PrecheckResultVO result = new PrecheckResultVO();
        result.setItems(items);
        boolean canTrigger = items.subList(0, 4).stream().allMatch(i -> ObjUtil.equals(i.getConclusion(), 1));
        result.setCanTrigger(canTrigger);
        if (active != null) {
            result.setMergedRunId(active.getId());
        }
        if (success != null) {
            result.setReusedRunId(success.getId());
            result.setArtifactDigest(null);
        }
        return result;
    }

    private boolean isRef(Integer refType, String commit) {
        if (StrUtil.isBlank(commit)) {
            return false;
        }
        if (Integer.valueOf(2).equals(refType)) {
            return REF_PATTERN.matcher(commit).matches();
        }
        return COMMIT_PATTERN.matcher(commit).matches();
    }

    /**
     * 解析本次触发的生效代码源：触发入参优先，未填写时回退应用登记的默认分支/提交。
     *
     * <p>生效值随运行落库并参与幂等键，保证页面预填值与 API 调用口径一致。
     * TODO(repo-client): 接入代码平台 API 后在此把 Tag/Ref 解析为确定 SHA，并校验提交属于分支。
     */
    private ResolvedSource resolveSource(App app, RunTriggerDTO body) {
        String appBranch = app == null ? null : app.getRepoBranch();
        String appCommit = app == null ? null : app.getRepoCommitId();
        boolean branchFromApp = StrUtil.isBlank(body.getBranch()) && StrUtil.isNotBlank(appBranch);
        boolean commitFromApp = StrUtil.isBlank(body.getCommitId()) && StrUtil.isNotBlank(appCommit);
        String branch = StrUtil.isNotBlank(body.getBranch()) ? body.getBranch() : appBranch;
        String commit = StrUtil.isNotBlank(body.getCommitId()) ? body.getCommitId() : appCommit;
        Integer refType = body.getRefType() == null ? IntConst.ONE : body.getRefType();
        return new ResolvedSource(branch, commit, refType, branchFromApp, commitFromApp);
    }

    private PrecheckItemVO item(int type, boolean pass, String detail) {
        PrecheckItemVO item = new PrecheckItemVO();
        item.setType(type);
        item.setConclusion(pass ? 1 : 2);
        item.setDetail(detail);
        return item;
    }

    private String firstFailDetail(PrecheckResultVO precheck) {
        return precheck.getItems().stream()
                .filter(i -> ObjUtil.equals(i.getConclusion(), 2))
                .map(PrecheckItemVO::getDetail).findFirst().orElse("预校验不通过");
    }

    private String idemKey(Long appId, String commit, Integer version) {
        return CryptoUtil.sha256Hex(appId + ":" + commit + ":" + version);
    }

    /**
     * 按版本快照创建阶段（全部未开始）
     */
    private void createStages(Long runId, Long pipelineId, Integer version, Integer fromSeq) {
        PipelineDefinitionDTO definition = pipelineStageService.loadDefinition(pipelineId, version);
        List<PipelineStageDTO> stages = definition.getStages();
        if (stages == null || stages.isEmpty()) {
            return;
        }
        int seq = 1;
        for (PipelineStageDTO stage : stages) {
            RunStage entity = new RunStage();
            entity.setRunId(runId);
            entity.setStageType(stage.getType());
            entity.setStageName(stage.getName());
            entity.setSeq(seq++);
            entity.setStatus(StageStatusEnum.NOT_STARTED.getCode());
            stageMapper.insert(entity);
            // 任务
            if (stage.getTasks() != null) {
                for (PipelineTaskDTO task : stage.getTasks()) {
                    RunTask runTask = new RunTask();
                    runTask.setRunId(runId);
                    runTask.setStageId(entity.getId());
                    runTask.setName(task.getName());
                    runTask.setExecutor(task.getExecutor());
                    runTask.setStatus(1);
                    runTask.setLogLineCount(0);
                    runTask.setIsTruncated(0);
                    taskMapper.insert(runTask);
                }
            }
        }
    }

    /**
     * 运行前检查版本定义的完整性：存在不可执行的空阶段/空任务时返回提示，否则返回 null。
     * 保存阶段放宽为空任务，仅在此处（触发运行）拦截并提示。
     *
     * @param pipelineId 流水线 id
     * @param version    流水线当前版本号
     * @return 不可运行的提示文本；定义可运行则为 null
     */
    private String checkRunnableDefinition(Long pipelineId, Integer version) {
        PipelineDefinitionDTO definition = pipelineStageService.loadDefinition(pipelineId, version);
        List<PipelineStageDTO> stages = definition.getStages();
        if (stages == null || stages.isEmpty()) {
            return "流水线定义未配置任何阶段";
        }
        int seq = 1;
        for (PipelineStageDTO stage : stages) {
            if (stage.getTasks() == null || stage.getTasks().isEmpty()) {
                return "第 " + seq + " 个阶段「" + stage.getName() + "」未添加任务，请先在画布中补充";
            }
            // CI 阶段（1-4）须至少一个 jenkins 任务带可执行命令，否则生成的脚本为空命令
            if (stage.getType() != null && stage.getType() <= 4) {
                boolean hasCommand = stage.getTasks().stream()
                        .filter(t -> t.getExecutor() == null || "jenkins".equalsIgnoreCase(t.getExecutor()))
                        .anyMatch(t -> StrUtil.isNotBlank(t.getScriptContent())
                                || StrUtil.isNotBlank(t.getScriptRef()));
                if (!hasCommand) {
                    return "第 " + seq + " 个 CI 阶段「" + stage.getName() + "」缺少可执行的 Jenkins 命令";
                }
            }
            seq++;
        }
        return null;
    }

    /**
     * 事务提交后触发 Jenkins 构建：避免轮询早于运行记录提交。
     */
    private void triggerJenkinsAfterCommit(Run run) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        triggerJenkinsBuild(run);
                    } catch (RuntimeException e) {
                        // 事务已提交，不能因触发异常回滚运行记录；留日志由人工/兜底处理
                        log.error("Jenkins trigger failed after commit: runId={}", run.getId(), e);
                    }
                }
            });
        } else {
            triggerJenkinsBuild(run);
        }
    }

    /**
     * 触发一次完整 CI 构建。一次运行只产生一个 Jenkins build，Jenkinsfile 内部顺序执行多个 CI 阶段；
     * 构建状态由 {@code RunPollingScheduler} 轮询更新。
     *
     * <p>明确失败（鉴权、Job 创建失败、4xx）直接置运行失败；可能已送达 Jenkins 的失败
     * （超时、连接中断、5xx）置「结果待确认」，由轮询任务继续尝试补正。
     */
    private void triggerJenkinsBuild(Run run) {
        Run latest = runMapper.selectById(run.getId());
        if (latest == null || !ObjUtil.equals(latest.getStatus(), RunStatusEnum.QUEUED.getCode())) {
            return;
        }
        App app = appMapper.selectById(latest.getAppId());
        if (app == null || StrUtil.isBlank(app.getCode())) {
            log.error("Jenkins trigger aborted: app missing, runId={}, appId={}", latest.getId(), latest.getAppId());
            markRunFailed(latest);
            return;
        }
        // 并发兜底：触发后若有并发保存推进版本或同步失败，Job 脚本与运行固定版本不一致，禁止旧脚本触发
        Pipeline pipeline = pipelineMapper.selectById(latest.getPipelineId());
        if (pipeline == null
                || !ObjUtil.equals(pipeline.getStatus(), PipelineStatusEnum.ENABLED.getCode())
                || !ObjUtil.equals(pipeline.getVersion(), latest.getPipelineVersion())) {
            log.warn("Jenkins trigger aborted: pipeline state changed, runId={}, runVersion={}, pipelineVersion={}, status={}",
                    latest.getId(), latest.getPipelineVersion(),
                    pipeline == null ? null : pipeline.getVersion(),
                    pipeline == null ? null : pipeline.getStatus());
            markRunFailed(latest);
            return;
        }
        String appCode = app.getCode().trim();
        String jobName = JenkinsJobNames.pipelineJobName(appCode, latest.getPipelineId());
        BuildSource source = new BuildSource(appCode, app.getRepoUrl(), latest.getBranch(),
                latest.getCommitId(), latest.getRefType(), latest.getRefValue());
        try {
            JenkinsDispatchResult result = jenkinsClient.triggerBuild(latest.getId(),
                    "snapshot:" + latest.getPipelineVersion(), jobName, source);
            latest.setJenkinsBuildNo(result.buildNo());
            latest.setJenkinsQueueId(result.queueId());
            // 记录实际构建的 Job 名，供轮询/中止使用；旧数据回退应用编码 Job
            latest.setJobName(result.jobName());
            runMapper.updateById(latest);
            log.info("Jenkins build triggered: runId={}, jobName={}, queueId={}, buildNo={}",
                    latest.getId(), result.jobName(), result.queueId(), result.buildNo());
        } catch (JenkinsDispatchException e) {
            log.warn("Jenkins trigger failed: runId={}, jobName={}, uncertain={}, reason={}",
                    latest.getId(), jobName, e.isUncertain(), e.getMessage());
            if (e.isUncertain()) {
                markRunUnknown(latest);
            } else {
                markRunFailed(latest);
            }
        }
    }

    /**
     * 运行实际构建的 Job 名：新运行记录于 run.job_name；旧数据回退应用编码 Job（旧命名协议）。
     */
    private String resolveRunJobName(Run run) {
        if (StrUtil.isNotBlank(run.getJobName())) {
            return run.getJobName();
        }
        App app = appMapper.selectById(run.getAppId());
        return app == null || StrUtil.isBlank(app.getCode()) ? null : app.getCode().trim();
    }


    private void markRunFailed(Run run) {
        Run latest = runMapper.selectById(run.getId());
        if (latest == null) {
            return;
        }
        latest.setStatus(RunStatusEnum.FAILURE.getCode());
        // 构建从未开始，不伪造开始时间与耗时，只记录失败截止时间
        latest.setEndTime(new Date());
        runMapper.updateById(latest);
        for (RunStage stage : stageMapper.selectByRun(run.getId())) {
            if (ObjUtil.equals(stage.getStatus(), StageStatusEnum.NOT_STARTED.getCode())) {
                stage.setStatus(StageStatusEnum.SKIPPED.getCode());
                stageMapper.updateById(stage);
            }
        }
    }

    private void markRunUnknown(Run run) {
        Run latest = runMapper.selectById(run.getId());
        if (latest != null && !ObjUtil.equals(latest.getStatus(), RunStatusEnum.UNKNOWN.getCode())) {
            latest.setStatus(RunStatusEnum.UNKNOWN.getCode());
            // TODO(reconcile): 兜底扫描任务尚未接入，当前仅记录截止时间，等待回调或人工处理
            latest.setResultExpireTime(new Date(System.currentTimeMillis() + 30L * 60 * 1000));
            runMapper.updateById(latest);
        }
    }

    private int resolveFromSeq(List<RunStage> stages, Long fromStageId) {
        if (ObjUtil.isEmpty(fromStageId)) {
            return stages.stream()
                    .filter(s -> ObjUtil.equals(s.getStatus(), StageStatusEnum.FAILURE.getCode()))
                    .map(RunStage::getSeq).findFirst().orElse(1);
        }
        return stages.stream().filter(s -> ObjUtil.equals(s.getId(), fromStageId))
                .map(RunStage::getSeq).findFirst().orElse(1);
    }

    private void persistPrechecks(Long runId, List<PrecheckItemVO> items) {
        for (PrecheckItemVO i : items) {
            RunPrecheck precheck = new RunPrecheck();
            precheck.setRunId(runId);
            precheck.setItemType(i.getType());
            precheck.setConclusion(i.getConclusion());
            precheck.setDetail(i.getDetail());
            precheck.setCheckedAt(new Date());
            precheckMapper.insert(precheck);
        }
    }

    private void writeTriggerRecord(RunTriggerDTO body, ResolvedSource source,
                                    int result, Long runId, String rejectReason) {
        RunTrigger record = new RunTrigger();
        record.setAppId(body.getAppId());
        record.setPipelineId(body.getPipelineId());
        record.setBranch(source.branch() == null ? "" : source.branch());
        record.setCommitId(source.commitId() == null ? "" : source.commitId());
        record.setTriggerUser(AuthUtil.getUserId());
        record.setResult(result);
        record.setRunId(runId);
        record.setRejectReason(rejectReason);
        triggerMapper.insert(record);
    }

    private void writeTriggerRecordFromRun(Run run, int result) {
        RunTrigger record = new RunTrigger();
        record.setAppId(run.getAppId());
        record.setPipelineId(run.getPipelineId());
        record.setBranch(run.getBranch());
        record.setCommitId(run.getCommitId());
        record.setTriggerUser(run.getTriggerUser());
        record.setResult(result);
        record.setRunId(run.getId());
        triggerMapper.insert(record);
    }

    private void writeOperation(Long runId, int type, String reason) {
        RunOperation op = new RunOperation();
        op.setRunId(runId);
        op.setType(type);
        op.setReason(reason);
        op.setOperator(AuthUtil.getUserId());
        operationMapper.insert(op);
    }

    private String generateRunNo() {
        String day = new SimpleDateFormat("yyyyMMdd").format(new Date());
        long seq = runMapper.nextSeqOfDay(day);
        return String.format("RUN-%s-%04d", day, seq);
    }

    private Run requireRun(Long id) {
        Run run = getById(id);
        if (run == null) {
            throw new ServiceException("运行不存在");
        }
        return run;
    }

    /**
     * 日志入库前脱敏（credential/token/key 遮蔽）。
     */
    static String mask(String line) {
        return SECRET_PATTERN.matcher(line).replaceAll("$1=******");
    }
}
