package com.ferry.devops.biz.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.RegexUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineStageTaskDTO;
import com.ferry.devops.api.dto.RunTriggerDTO;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunOperation;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.entity.RunStageTask;
import com.ferry.devops.api.enums.DevopsStateMachine;
import com.ferry.devops.api.enums.PipelineStatusEnum;
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.api.enums.StageStatusEnum;
import com.ferry.devops.api.enums.TriggerResultEnum;
import com.ferry.devops.api.vo.PrecheckItemVO;
import com.ferry.devops.api.vo.PrecheckResultVO;
import com.ferry.devops.api.vo.RunDetailVO;
import com.ferry.devops.api.vo.RunStageVO;
import com.ferry.devops.api.vo.RunStageTaskVO;
import com.ferry.devops.api.vo.RunVO;
import com.ferry.devops.api.vo.TriggerResultVO;
import com.ferry.devops.api.vo.VersionVO;
import com.ferry.devops.biz.integration.jenkins.BuildSource;
import com.ferry.devops.biz.integration.jenkins.JenkinsClient;
import com.ferry.devops.biz.integration.jenkins.JenkinsDispatchException;
import com.ferry.devops.biz.integration.jenkins.JenkinsDispatchResult;
import com.ferry.devops.biz.integration.jenkins.JenkinsJobNames;
import com.ferry.devops.biz.mapper.AppMapper;
import com.ferry.devops.biz.mapper.PipelineMapper;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunOperationMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.mapper.RunStageTaskMapper;
import com.ferry.devops.biz.service.IPipelineService;
import com.ferry.devops.biz.service.IPipelineStageService;
import com.ferry.devops.biz.service.IRunService;
import com.ferry.devops.biz.service.IRunStageService;
import com.ferry.devops.biz.wrapper.RunWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 运行服务实现：五项预校验、触发、阶段编排、日志脱敏分段、断点重跑、运行干预。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunServiceImpl extends ServiceImpl<RunMapper, Run> implements IRunService {
    /** 提交 id：7-40 位十六进制 */
    private static final String COMMIT_REGEX = "^[0-9a-fA-F]{7,40}$";
    /** Git ref/tag：不含空白与 ~^:?*[\ 等非法字符，最长 100 */
    private static final String REF_REGEX = "^[^\\s~^:?*\\[\\\\]{1,100}$";
    private static final int PRE_CHECK_RESULT_MAX = 500;
    // 运行任务表的 executor 是 varchar，仅在创建运行快照时转换定义表的数字码。
    private static final Map<Integer, String> EXECUTOR_NAMES = Map.of(
            1, "jenkins", 2, "deployer", 3, "gate", 4, "release");

    private final RunMapper runMapper;
    private final PipelineMapper pipelineMapper;
    private final IPipelineStageService pipelineStageService;
    private final RunStageMapper stageMapper;
    private final RunStageTaskMapper taskMapper;
    private final RunOperationMapper operationMapper;
    private final JenkinsClient jenkinsClient;
    private final AppMapper appMapper;
    private final IPipelineService pipelineService;
    private final IRunStageService runStageService;

    /**
     * 触发生效的代码源。
     *
     * @param branch        生效分支
     * @param commitId      生效提交或引用值
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
        List<RunStageTask> tasks = taskMapper.selectByRun(id);
        Map<Long, List<RunStageTask>> tasksByStage = tasks.stream()
                .collect(Collectors.groupingBy(RunStageTask::getRunStageId));
        List<RunStageVO> stageVos = stages.stream().map(s -> {
            RunStageVO sv = BeanUtil.copy(s, RunStageVO.class);
            sv.setTasks(tasksByStage.getOrDefault(s.getId(), Collections.emptyList()).stream()
                    .map(t -> BeanUtil.copy(t, RunStageTaskVO.class)).collect(Collectors.toList()));
            return sv;
        }).collect(Collectors.toList());
        vo.setStages(stageVos);
        vo.setTasks(tasks.stream().map(t -> BeanUtil.copy(t, RunStageTaskVO.class)).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TriggerResultVO trigger(RunTriggerDTO body) {
        // 频率管控：同一用户同一应用 60 秒内第 6 次触发拒绝
        if (runMapper.countByUserSince(body.getAppId(), AuthUtil.getUserId(),
                new Date(System.currentTimeMillis() - 60_000L)) >= 5) {
            throw new ServiceException("操作过于频繁，请稍后再试");
        }
        // 生效代码源只解析一次：预校验、运行落库、触发记录共用
        App app = appMapper.selectById(body.getAppId());
        ResolvedSource source = resolveSource(app, body);
        PrecheckResultVO precheck = doPrecheck(body, app, source);
        Pipeline pipeline = pipelineMapper.selectById(body.getPipelineId());

        // 确定性五项不通过：仍落一条运行记录，沉淀拒绝原因，便于列表回溯
        if (!itemsPassed(precheck)) {
            String rejectReason = firstFailDetail(precheck);
            Run rejected = newRunRecord(body, source, pipeline, app);
            rejected.setStatus(RunStatusEnum.PRECHECK.getCode());
            rejected.setPreCheckResult(truncatePreCheckResult(buildPreCheckResult(precheck)));
            save(rejected);
            TriggerResultVO vo = new TriggerResultVO();
            vo.setResult(TriggerResultEnum.REJECTED.getCode());
            vo.setRunId(rejected.getId());
            vo.setMessage("预校验不通过：" + rejectReason);
            return vo;
        }
        // 命中活动运行直接合并
        if (ObjUtil.isNotEmpty(precheck.getMergedRunId())) {
            TriggerResultVO vo = new TriggerResultVO();
            vo.setResult(TriggerResultEnum.MERGED.getCode());
            vo.setRunId(precheck.getMergedRunId());
            vo.setMessage("命中幂等键，已合并到进行中的运行");
            return vo;
        }

        // AI 研判已关闭：五项规则通过后直接创建运行
        Run run = newRunRecord(body, source, pipeline, app);
        run.setStatus(RunStatusEnum.QUEUED.getCode());
        run.setPreCheckResult(truncatePreCheckResult(buildPreCheckResult(precheck)));
        save(run);
        createStages(run.getId(), pipeline.getId(), pipeline.getVersion(), precheck);
        triggerJenkinsAfterCommit(run);
        TriggerResultVO vo = new TriggerResultVO();
        vo.setResult(TriggerResultEnum.CREATED.getCode());
        vo.setRunId(run.getId());
        vo.setMessage("运行已创建，状态排队中");
        return vo;
    }

    /**
     * 构造运行记录骨架：触发信息与预检无关，成功与被拒绝两条路径共用。
     */
    private Run newRunRecord(RunTriggerDTO body, ResolvedSource source, Pipeline pipeline, App app) {
        Run run = new Run();
        run.setCode(generateCode());
        run.setAppId(body.getAppId());
        run.setPipelineId(body.getPipelineId());
        run.setPipelineVersion(pipeline == null ? null : pipeline.getVersion());
        run.setBranch(source.branch());
        run.setRefType(source.refType());
        run.setRefValue(source.commitId());
        run.setTriggerType(IntConst.ONE);
        run.setTriggerUser(AuthUtil.getUserId());
        run.setTriggerNote(body.getNote());
        return run;
    }

    /**
     * 预检内容按表字段上限截断，避免超长写入失败。
     */
    private String truncatePreCheckResult(String content) {
        return content == null || content.length() <= PRE_CHECK_RESULT_MAX
                ? content : content.substring(0, PRE_CHECK_RESULT_MAX);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pause(Long id, String reason) {
        Run run = requireRun(id);
        if (!ObjUtil.equals(run.getStatus(), RunStatusEnum.RUNNING.getCode())
                && !ObjUtil.equals(run.getStatus(), RunStatusEnum.QUEUED.getCode())) {
            throw new ServiceException("仅运行中或排队中的运行可暂停");
        }
        DevopsStateMachine.checkRun(run.getStatus(), RunStatusEnum.BLOCKED.getCode());
        run.setStatus(RunStatusEnum.BLOCKED.getCode());
        updateById(run);
        writeOperation(id, 1, reason);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resume(Long id) {
        Run run = requireRun(id);
        if (!ObjUtil.equals(run.getStatus(), RunStatusEnum.BLOCKED.getCode())) {
            throw new ServiceException("仅阻塞的运行可继续");
        }
        if (!runStageService.canResumeManually(id)) {
            throw new ServiceException("门禁未通过或存在待处理阶段，请先处理或申请豁免");
        }
        DevopsStateMachine.checkRun(run.getStatus(), RunStatusEnum.RUNNING.getCode());
        run.setStatus(RunStatusEnum.RUNNING.getCode());
        updateById(run);
        writeOperation(id, 2, null);
        runStageService.refreshRunStatus(id);
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
        return stageMapper.selectByRun(id).stream().map(s -> {
            RunStageVO vo = BeanUtil.copy(s, RunStageVO.class);
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
        int fromSort = resolveFromSort(oldStages, body.getFromStageId());
        // 旧版本重跑先回退生成当前版本
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
        run.setCode(generateCode());
        run.setAppId(original.getAppId());
        run.setPipelineId(original.getPipelineId());
        run.setPipelineVersion(pipelineMapper.selectById(original.getPipelineId()).getVersion());
        run.setBranch(original.getBranch());
        run.setRefType(original.getRefType());
        run.setRefValue(original.getRefValue());
        run.setTriggerType(IntConst.ONE);
        run.setTriggerUser(AuthUtil.getUserId());
        run.setTriggerNote(body.getNote());
        run.setStatus(RunStatusEnum.QUEUED.getCode());
        run.setPreCheckResult("断点重跑");
        save(run);
        // 阶段：复用成功阶段（status=8 已复用），其余未开始
        int sort = 1;
        for (RunStage old : oldStages) {
            RunStage stage = new RunStage();
            stage.setRunId(run.getId());
            stage.setType(old.getType());
            stage.setName(old.getName());
            stage.setSort(sort++);
            boolean reused = old.getSort() < fromSort
                    && ObjUtil.equals(old.getStatus(), StageStatusEnum.SUCCESS.getCode());
            if (reused) {
                stage.setStatus(StageStatusEnum.REUSED.getCode());
                stage.setStartTime(old.getStartTime());
                stage.setEndTime(old.getEndTime());
            } else {
                stage.setStatus(StageStatusEnum.NOT_STARTED.getCode());
            }
            stageMapper.insert(stage);
            createRerunTasks(run.getId(), stage.getId(), old.getId(), reused);
        }
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
     */
    private PrecheckResultVO doPrecheck(RunTriggerDTO body, App app, ResolvedSource source) {
        Long appId = body.getAppId();
        Long pipelineId = body.getPipelineId();
        List<PrecheckItemVO> items = new ArrayList<>();

        // 1 仓库信息
        PrecheckItemVO repoItem;
        if (app == null) {
            repoItem = item(1, false, "应用不存在");
        } else if (StrUtil.isBlank(app.getRepoUrl())) {
            repoItem = item(1, false, "应用未填写仓库地址");
        } else {
            repoItem = item(1, true, "仓库信息已登记：" + app.getRepoUrl());
        }
        items.add(repoItem);

        // 2 分支存在
        String branchDetail;
        if (StrUtil.isBlank(source.branch())) {
            branchDetail = "分支不能为空，且应用未登记默认分支";
        } else if (source.branchFromApp()) {
            branchDetail = "未指定分支，使用应用默认分支：" + source.branch();
        } else {
            branchDetail = "分支已指定：" + source.branch();
        }
        items.add(item(2, StrUtil.isNotBlank(source.branch()), branchDetail));

        // 3 提交可达
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

        // 4 定义匹配
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
            defDetail = checkRunnableDefinition(pipeline.getId(), pipeline.getVersion());
            if (defDetail == null) {
                // 定义本身齐备后，真实探测 Jenkins Job：避免预检说"已同步"、触发却 404
                String jobName = app == null ? null : JenkinsJobNames.appJobName(app.getCode());
                if (!jenkinsClient.jobExists(jobName)) {
                    defOk = false;
                    defDetail = "Jenkins Job「" + jobName + "」不存在，请到流水线重新保存触发同步";
                } else {
                    defDetail = "流水线已启用且 Jenkins Job 已就绪";
                }
            } else {
                defOk = false;
            }
        }
        items.add(item(4, defOk, defDetail));

        // 5 幂等检查：按应用+引用+流水线版本查活动/成功运行
        Integer version = pipeline == null ? null : pipeline.getVersion();
        Run active = runMapper.selectActiveBySource(appId, source.commitId(), version);
        Run success = active != null ? null : runMapper.selectSuccessBySource(appId, source.commitId(), version);
        PrecheckItemVO idemItem = new PrecheckItemVO();
        idemItem.setType(5);
        if (active != null) {
            idemItem.setConclusion(3);
            idemItem.setDetail("命中活动运行，将合并到运行 " + active.getId());
        } else if (success != null) {
            idemItem.setConclusion(1);
            idemItem.setDetail("同提交存在成功运行 " + success.getId() + "，本次新建运行，其制品可在制品管理页复用");
        } else {
            idemItem.setConclusion(1);
            idemItem.setDetail("未命中活动运行，将创建新运行");
        }
        items.add(idemItem);

        PrecheckResultVO result = new PrecheckResultVO();
        result.setItems(items);
        boolean rulesPassed = items.subList(0, 4).stream().allMatch(i -> ObjUtil.equals(i.getConclusion(), 1));

        // AI 风险研判前置到预检：五项规则通过后再问模型，结论随预检结果一并返回并复用给 trigger
        // boolean jobExists = items.get(3) != null
        //         && ObjUtil.equals(items.get(3).getConclusion(), 1);
        // AiPrecheckAssistant.RiskVerdict verdict = aiPrecheckAssistant.assess(
        //         app == null ? "" : app.getName(),
        //         pipeline == null ? "" : pipeline.getName(),
        //         app == null ? "" : app.getRepoUrl(),
        //         source.branch(), source.commitId(), items, jobExists);
        // result.setAiPassed(verdict.passed());
        // result.setAiReason(verdict.reason());
        // result.setCanTrigger(rulesPassed && verdict.passed());
        result.setCanTrigger(rulesPassed);

        if (active != null) {
            result.setMergedRunId(active.getId());
        }
        return result;
    }

    private boolean isRef(Integer refType, String commit) {
        if (StrUtil.isBlank(commit)) {
            return false;
        }
        if (Integer.valueOf(2).equals(refType)) {
            return RegexUtil.matches(REF_REGEX, commit);
        }
        return RegexUtil.matches(COMMIT_REGEX, commit);
    }

    /**
     * 解析本次触发的生效代码源：触发入参优先，未填写时回退应用登记的默认分支/提交。
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

    /** 五项确定性规则是否全部通过（不含 AI 研判结论） */
    private boolean itemsPassed(PrecheckResultVO precheck) {
        List<PrecheckItemVO> items = precheck.getItems();
        return items != null && items.subList(0, Math.min(4, items.size())).stream()
                .allMatch(i -> ObjUtil.equals(i.getConclusion(), 1));
    }

    private String firstFailDetail(PrecheckResultVO precheck) {
        return precheck.getItems().stream()
                .filter(i -> ObjUtil.equals(i.getConclusion(), 2))
                .map(PrecheckItemVO::getDetail).findFirst().orElse("预校验不通过");
    }

    /**
     * 拼装预检结果摘要：逐项类型与明细，超长按字段上限截断。
     */
    private String buildPreCheckResult(PrecheckResultVO precheck) {
        String content = precheck.getItems().stream()
                .map(item -> item.getType() + ":" + item.getDetail())
                .collect(Collectors.joining("；"));
        return content.length() > PRE_CHECK_RESULT_MAX
                ? content.substring(0, PRE_CHECK_RESULT_MAX) : content;
    }

    /**
     * 按版本快照创建阶段与任务：首个阶段为预检（已通过，result 存五项结论），其余未开始。
     */
    private void createStages(Long runId, Long pipelineId, Integer version, PrecheckResultVO precheck) {
        PipelineDefinitionDTO definition = pipelineStageService.loadDefinition(pipelineId, version);
        List<PipelineStageDTO> stages = definition.getStages();
        if (stages == null || stages.isEmpty()) {
            return;
        }
        int sort = 1;
        for (PipelineStageDTO stage : stages) {
            RunStage entity = new RunStage();
            entity.setRunId(runId);
            entity.setType(stage.getType());
            entity.setName(stage.getName());
            entity.setSort(sort++);
            // 预检阶段由定义中的 type=0 配置驱动：触发前五项校验已通过，落为成功并留痕
            boolean precheckStage = Integer.valueOf(0).equals(stage.getType());
            entity.setStatus(precheckStage
                    ? StageStatusEnum.SUCCESS.getCode()
                    : StageStatusEnum.NOT_STARTED.getCode());
            if (precheckStage) {
                entity.setResult(com.ferry.common.core.utils.JsonUtil.toJson(precheck.getItems()));
            }
            stageMapper.insert(entity);
            if (precheckStage) {
                // 预检结论由服务端生成，不配置任务
                continue;
            }
            int taskSort = 1;
            if (stage.getTasks() != null) {
                for (PipelineStageTaskDTO task : stage.getTasks()) {
                    RunStageTask runTask = new RunStageTask();
                    runTask.setRunId(runId);
                    runTask.setRunStageId(entity.getId());
                    runTask.setName(task.getName());
                    Integer executor = task.getExecutor() == null ? IntConst.ONE : task.getExecutor();
                    runTask.setExecutor(EXECUTOR_NAMES.getOrDefault(executor, "jenkins"));
                    runTask.setScriptRef(task.getScriptRef());
                    runTask.setScriptContent(task.getScriptContent());
                    runTask.setStatus(1);
                    runTask.setSort(taskSort++);
                    taskMapper.insert(runTask);
                }
            }
        }
    }

    /**
     * 断点重跑：按原阶段任务快照复制任务，保持任务顺序与脚本内容。
     */
    private void createRerunTasks(Long runId, Long runStageId, Long oldRunStageId, boolean reused) {
        List<RunStageTask> oldTasks = taskMapper.selectByStage(oldRunStageId);
        int sort = 1;
        for (RunStageTask old : oldTasks) {
            RunStageTask task = new RunStageTask();
            task.setRunId(runId);
            task.setRunStageId(runStageId);
            task.setName(old.getName());
            task.setExecutor(old.getExecutor());
            task.setScriptRef(old.getScriptRef());
            task.setScriptContent(old.getScriptContent());
            task.setStatus(reused ? old.getStatus() : 1);
            task.setStartTime(reused ? old.getStartTime() : null);
            task.setEndTime(reused ? old.getEndTime() : null);
            task.setSort(sort++);
            taskMapper.insert(task);
        }
    }

    /**
     * 运行前检查版本定义的完整性。
     */
    private String checkRunnableDefinition(Long pipelineId, Integer version) {
        PipelineDefinitionDTO definition = pipelineStageService.loadDefinition(pipelineId, version);
        List<PipelineStageDTO> stages = definition.getStages();
        if (stages == null || stages.isEmpty()) {
            return "流水线定义未配置任何阶段";
        }
        int seq = 1;
        for (PipelineStageDTO stage : stages) {
            // 预检阶段由服务端生成五项结论，无需配置任务
            if (Integer.valueOf(0).equals(stage.getType())) {
                seq++;
                continue;
            }
            if (stage.getTasks() == null || stage.getTasks().isEmpty()) {
                return "第 " + seq + " 个阶段「" + stage.getName() + "」未添加任务，请先在画布中补充";
            }
            if (stage.getType() != null && stage.getType() >= 1 && stage.getType() <= 4) {
                boolean hasCommand = stage.getTasks().stream()
                        .filter(t -> t.getExecutor() == null || IntConst.ONE.equals(t.getExecutor()))
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
     * 事务提交后触发 Jenkins 构建。
     */
    private void triggerJenkinsAfterCommit(Run run) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        triggerJenkinsBuild(run);
                    } catch (RuntimeException e) {
                        log.error("Jenkins trigger failed after commit: runId={}", run.getId(), e);
                    }
                }
            });
        } else {
            triggerJenkinsBuild(run);
        }
    }

    /**
     * 触发一次完整 CI 构建。
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
        String jobName = JenkinsJobNames.appJobName(appCode);
        BuildSource source = new BuildSource(appCode, app.getRepoUrl(), latest.getBranch(),
                latest.getRefValue(), latest.getRefType(), latest.getRefValue());
        try {
            JenkinsDispatchResult result = jenkinsClient.triggerBuild(latest.getId(),
                    "snapshot:" + latest.getPipelineVersion(), jobName, source);
            latest.setJenkinsBuildNo(result.buildNo());
            latest.setJenkinsQueueId(result.queueId());
            latest.setJenkinsJobId(result.jobName());
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
     * 运行实际构建的 Job 名：优先运行记录的 jenkins_job_id，旧数据回退应用编码 Job。
     */
    private String resolveRunJobName(Run run) {
        if (StrUtil.isNotBlank(run.getJenkinsJobId())) {
            return run.getJenkinsJobId();
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
            runMapper.updateById(latest);
        }
    }

    private int resolveFromSort(List<RunStage> stages, Long fromStageId) {
        if (ObjUtil.isEmpty(fromStageId)) {
            return stages.stream()
                    .filter(s -> ObjUtil.equals(s.getStatus(), StageStatusEnum.FAILURE.getCode()))
                    .map(RunStage::getSort).findFirst().orElse(1);
        }
        return stages.stream().filter(s -> ObjUtil.equals(s.getId(), fromStageId))
                .map(RunStage::getSort).findFirst().orElse(1);
    }

    private void writeOperation(Long runId, int type, String reason) {
        RunOperation op = new RunOperation();
        op.setRunId(runId);
        op.setType(type);
        op.setReason(reason);
        op.setOperator(AuthUtil.getUserId());
        operationMapper.insert(op);
    }

    private String generateCode() {
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
}
