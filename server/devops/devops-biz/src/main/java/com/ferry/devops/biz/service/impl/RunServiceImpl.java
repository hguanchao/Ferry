package com.ferry.devops.biz.service.impl;
import com.ferry.devops.api.vo.RunVO;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CryptoUtil;
import com.ferry.common.core.utils.JsonUtil;
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
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.api.enums.TriggerResultEnum;
import com.ferry.devops.api.vo.PrecheckItemVO;
import com.ferry.devops.api.vo.PrecheckResultVO;
import com.ferry.devops.api.vo.RunDetailVO;
import com.ferry.devops.api.vo.RunStageVO;
import com.ferry.devops.api.vo.RunTaskVO;
import com.ferry.devops.api.vo.TriggerResultVO;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
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
import com.ferry.devops.biz.service.IPipelineStageService;
import com.ferry.devops.biz.service.IRunService;
import com.ferry.devops.biz.wrapper.RunWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return doPrecheck(body);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TriggerResultVO trigger(RunTriggerDTO body) {
        // 频率管控：同一用户同一应用 60 秒内第 6 次触发拒绝
        if (runMapper.countByUserSince(body.getAppId(), AuthUtil.getUserId(),
                new Date(System.currentTimeMillis() - 60_000L)) >= 5) {
            throw new ServiceException("操作过于频繁，请稍后再试");
        }
        PrecheckResultVO precheck = doPrecheck(body);
        if (!Boolean.TRUE.equals(precheck.getCanTrigger())) {
            writeTriggerRecord(body, 4, null, firstFailDetail(precheck));
            TriggerResultVO vo = new TriggerResultVO();
            vo.setResult(TriggerResultEnum.REJECTED.getCode());
            vo.setMessage("预校验不通过：" + firstFailDetail(precheck));
            return vo;
        }
        // 幂等命中
        if (ObjUtil.isNotEmpty(precheck.getMergedRunId())) {
            writeTriggerRecord(body, 2, precheck.getMergedRunId(), null);
            TriggerResultVO vo = new TriggerResultVO();
            vo.setResult(TriggerResultEnum.MERGED.getCode());
            vo.setRunId(precheck.getMergedRunId());
            vo.setMessage("命中幂等键，已合并到进行中的运行");
            return vo;
        }
        if (ObjUtil.isNotEmpty(precheck.getReusedRunId())) {
            writeTriggerRecord(body, 3, precheck.getReusedRunId(), null);
            TriggerResultVO vo = new TriggerResultVO();
            vo.setResult(TriggerResultEnum.REUSED.getCode());
            vo.setRunId(precheck.getReusedRunId());
            vo.setMessage("命中成功运行，将复用其制品");
            return vo;
        }
        // 幂等键
        Pipeline pipeline = pipelineMapper.selectById(body.getPipelineId());
        String idemKey = idemKey(body.getAppId(), body.getBranch(), body.getCommitId(), pipeline.getVersion());
        // 事务内创建运行 + 阶段 + 触发记录 + 幂等记录
        Run run = new Run();
        run.setRunNo(generateRunNo());
        run.setPipelineId(body.getPipelineId());
        run.setPipelineVersion(pipeline.getVersion());
        run.setAppId(body.getAppId());
        run.setBranch(body.getBranch());
        run.setCommitId(body.getCommitId());
        run.setRefType(body.getRefType() == null ? IntConst.ONE : body.getRefType());
        run.setRefValue(body.getCommitId());
        run.setTriggerType(IntConst.ONE);
        run.setTriggerUser(AuthUtil.getUserId());
        run.setTriggerNote(body.getNote());
        run.setStatus(RunStatusEnum.QUEUED.getCode());
        run.setIsPrecheckPass(IntConst.ONE);
        run.setIdempotentKey(idemKey);
        save(run);
        createStages(run.getId(), pipeline.getId(), pipeline.getVersion(), null);
        persistPrechecks(run.getId(), precheck.getItems());
        writeTriggerRecord(body, 1, run.getId(), null);
        RunIdempotency idem = new RunIdempotency();
        idem.setIdempotentKey(idemKey);
        idem.setAppId(body.getAppId());
        idem.setRunId(run.getId());
        idem.setState(IntConst.ONE);
        idem.setExpireTime(new Date(System.currentTimeMillis() + IDEMPotent_WINDOW_SECONDS * 1000L));
        idempotencyMapper.insert(idem);
        // 事务提交后由 JobDispatchService 异步下发；最小实现记日志
        dispatchStages(run.getId(), pipeline.getVersion());
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
            jenkinsClient.abortBuild(id, run.getJenkinsBuildNo());
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
        // 新建运行（不覆盖原运行）
        Run run = new Run();
        run.setRunNo(generateRunNo());
        run.setPipelineId(original.getPipelineId());
        run.setPipelineVersion(original.getPipelineVersion());
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
        dispatchStages(run.getId(), run.getPipelineVersion());
        TriggerResultVO vo = new TriggerResultVO();
        vo.setResult(TriggerResultEnum.CREATED.getCode());
        vo.setRunId(run.getId());
        vo.setMessage("断点重跑已创建，前序成功阶段已复用");
        return vo;
    }

    // ==================== 内部实现 ====================

    /**
     * 五项预校验（仓库信息、分支、提交、定义匹配、幂等检查）。
     */
    private PrecheckResultVO doPrecheck(RunTriggerDTO body) {
        Long appId = body.getAppId();
        Long pipelineId = body.getPipelineId();
        String branch = body.getBranch();
        String commitId = body.getCommitId();
        List<PrecheckItemVO> items = new ArrayList<>();
        // TODO(dev_app-schema): dev_app_repo 已删除，仓库信息改为读取 dev_app 内联字段 repo_url。
        App app = appMapper.selectById(appId);

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

        // 2 分支存在：校验触发时指定分支非空
        items.add(item(2, StrUtil.isNotBlank(branch), StrUtil.isNotBlank(branch)
                ? "分支已指定：" + branch : "分支不能为空"));

        // 3 提交可达：校验 7–40 位 hex 或合法 Git ref
        boolean commitOk = isRef(body);
        items.add(item(3, commitOk, commitOk
                ? "提交引用格式合法" : "提交必须为 7–40 位十六进制或合法 Git ref"));

        // 4 定义匹配：流水线启用 + 存在当前版本 + 定义中无空阶段/空任务
        Pipeline pipeline = pipelineMapper.selectById(pipelineId);
        boolean defOk = pipeline != null && ObjUtil.equals(pipeline.getStatus(), IntConst.ONE)
                && ObjUtil.isNotEmpty(pipeline.getVersion());
        String defDetail;
        if (!defOk) {
            defDetail = "流水线未启用或无当前版本";
        } else {
            // 保存阶段允许空任务，运行时才拦截：给出明确提示让用户在画布补齐后再触发
            defDetail = checkRunnableDefinition(pipeline.getId(), pipeline.getVersion());
            if (defDetail == null) {
                defDetail = "流水线已启用且存在当前版本";
            } else {
                defOk = false;
            }
        }
        items.add(item(4, defOk, defDetail));
        // 5 幂等检查：按 60 秒键查活动/成功运行
        String idemKey = idemKey(appId, branch, commitId,
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

    private boolean isRef(RunTriggerDTO body) {
        String commit = body.getCommitId();
        if (StrUtil.isBlank(commit)) {
            return false;
        }
        if (Integer.valueOf(2).equals(body.getRefType())) {
            return REF_PATTERN.matcher(commit).matches();
        }
        return COMMIT_PATTERN.matcher(commit).matches();
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

    private String idemKey(Long appId, String branch, String commit, Integer version) {
        return CryptoUtil.sha256Hex(appId + ":" + branch + ":" + commit + ":" + version);
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
            seq++;
        }
        return null;
    }

    /**
     * 事务提交后下发 CI 阶段 Job；真实 HTTP 由 JenkinsClient 完成，失败置「结果待确认」不回滚运行记录。
     */
    private void dispatchStages(Long runId, Integer version) {
        List<RunStage> stages = stageMapper.selectByRun(runId);
        boolean dispatchFailed = false;
        for (RunStage stage : stages) {
            if (ObjUtil.equals(stage.getStatus(), StageStatusEnum.NOT_STARTED.getCode())
                    && stage.getStageType() <= 4) {
                String jobId = runId + "-" + stage.getId();
                stage.setJenkinsJobId(jobId);
                stageMapper.updateById(stage);
                try {
                    jenkinsClient.dispatchStage(runId, stage.getId(), jobId, "snapshot:" + version);
                } catch (ServiceException e) {
                    // 真实 Jenkins 下发失败：不抛出以免回滚运行记录，记录告警并置结果待确认
                    dispatchFailed = true;
                }
            }
        }
        if (dispatchFailed) {
            Run run = runMapper.selectById(runId);
            if (run != null && !ObjUtil.equals(run.getStatus(), RunStatusEnum.UNKNOWN.getCode())) {
                run.setStatus(RunStatusEnum.UNKNOWN.getCode());
                // 结果待确认 30 分钟后由兜底任务补查
                run.setResultExpireTime(new Date(System.currentTimeMillis() + 30L * 60 * 1000));
                runMapper.updateById(run);
            }
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

    private void writeTriggerRecord(RunTriggerDTO body, int result, Long runId, String rejectReason) {
        RunTrigger record = new RunTrigger();
        record.setAppId(body.getAppId());
        record.setPipelineId(body.getPipelineId());
        record.setBranch(body.getBranch());
        record.setCommitId(body.getCommitId());
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
