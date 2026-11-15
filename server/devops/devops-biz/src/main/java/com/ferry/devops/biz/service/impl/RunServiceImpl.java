package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.cache.utils.RedisUtil;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CryptoUtil;
import com.ferry.common.core.utils.DateUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.RegexUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.config.props.SecurityProperties;
import com.ferry.common.security.model.AuthUser;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.RunOptionDTO;
import com.ferry.devops.api.dto.RunTriggerDTO;
import com.ferry.devops.api.dto.query.RunQuery;
import com.ferry.devops.api.dto.query.RunTriggerRecordQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.entity.PipelineVersion;
import com.ferry.devops.api.entity.Run;
import com.ferry.devops.api.entity.RunStage;
import com.ferry.devops.api.entity.RunTrigger;
import com.ferry.devops.api.enums.PrecheckConclusionEnum;
import com.ferry.devops.api.enums.RunStageStatusEnum;
import com.ferry.devops.api.enums.RunStatusEnum;
import com.ferry.devops.api.enums.TriggerResultEnum;
import com.ferry.devops.api.vo.CommitBriefVO;
import com.ferry.devops.api.vo.PipelineDefinitionVO;
import com.ferry.devops.api.vo.PipelineStageVO;
import com.ferry.devops.api.vo.PrecheckItemVO;
import com.ferry.devops.api.vo.RunDetailVO;
import com.ferry.devops.api.vo.RunStageVO;
import com.ferry.devops.api.vo.RunTaskVO;
import com.ferry.devops.api.vo.RunVO;
import com.ferry.devops.api.vo.TriggerRecordVO;
import com.ferry.devops.api.vo.TriggerResultVO;
import com.ferry.devops.biz.mapper.RunMapper;
import com.ferry.devops.biz.mapper.RunStageMapper;
import com.ferry.devops.biz.mapper.RunTriggerMapper;
import com.ferry.devops.biz.platform.CodePlatformUtil;
import com.ferry.devops.biz.platform.CommitBrief;
import com.ferry.devops.biz.service.IAppService;
import com.ferry.devops.biz.service.IPipelineService;
import com.ferry.devops.biz.service.IPipelineVersionService;
import com.ferry.devops.biz.service.IRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RunServiceImpl extends ServiceImpl<RunMapper, Run> implements IRunService {

    private static final int COMMIT_OPTION_LIMIT = 10;
    private static final int TRIGGER_RATE_LIMIT = 5;
    private static final long RATE_LIMIT_SECONDS = 60;

    /**
     * 幂等命中时视为进行中的运行状态
     */
    private static final Set<String> ACTIVE_STATUSES = Set.of(
            RunStatusEnum.QUEUED.getCode(), RunStatusEnum.RUNNING.getCode(), RunStatusEnum.BLOCKED.getCode());

    private final RunStageMapper runStageMapper;
    private final RunTriggerMapper runTriggerMapper;
    private final IAppService appService;
    private final IPipelineService pipelineService;
    private final IPipelineVersionService pipelineVersionService;
    private final SecurityProperties securityProperties;

    @Override
    public List<String> branchOptions(RunOptionDTO body) {
        App app = loadApp(body.getAppId());
        return CodePlatformUtil.listBranches(app.getRepoType(), app.getRepoUrl(), decryptToken(app));
    }

    @Override
    public List<CommitBriefVO> commitOptions(RunOptionDTO body) {
        App app = loadApp(body.getAppId());
        if (StrUtil.isBlank(body.getBranch())) {
            throw new ServiceException("请先选择分支");
        }
        List<CommitBrief> commits = CodePlatformUtil.listCommits(app.getRepoType(), app.getRepoUrl(),
                body.getBranch(), decryptToken(app), COMMIT_OPTION_LIMIT);
        List<CommitBriefVO> vos = BeanUtil.copy(commits, CommitBriefVO.class);
        for (int i = 0; i < vos.size(); i++) {
            Date time = commits.get(i).committedTime();
            vos.get(i).setCommittedTime(time == null ? null : DateUtil.format(time, DateUtil.NORM_DATETIME_PATTERN));
        }
        return vos;
    }

    @Override
    public List<PrecheckItemVO> precheck(RunTriggerDTO body) {
        return collectPrechecks(body);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TriggerResultVO trigger(RunTriggerDTO body) {
        rateLimit(body.getAppId());
        List<PrecheckItemVO> items = collectPrechecks(body);
        PrecheckItemVO fail = items.stream()
                .filter(item -> PrecheckConclusionEnum.FAIL.getCode().equals(item.getConclusion()))
                .findFirst().orElse(null);
        // 预校验不通过：返回「已拒绝」及原因，触发记录在本事务内落库作为审计
        if (fail != null) {
            recordTrigger(body, TriggerResultEnum.REJECTED, fail.getItemName() + "：" + fail.getDetail(), null);
            return buildResult(TriggerResultEnum.REJECTED, null, null, fail.getItemName() + "：" + fail.getDetail());
        }

        Pipeline pipeline = pipelineService.getById(body.getPipelineId());
        App app = appService.getById(body.getAppId());
        // 触发落库需要提交人与时间，预校验已验证可达，这里按同一口径再取一次
        CommitBrief commit = null;
        if (IntConst.ZERO.equals(body.getRefType())) {
            commit = CodePlatformUtil.getCommit(app.getRepoType(), app.getRepoUrl(),
                    body.getCommitId(), decryptToken(app));
        }

        Run existing = findLatestRun(body, pipeline);
        if (existing != null && ACTIVE_STATUSES.contains(existing.getStatus())) {
            // 幂等合并：命中进行中的运行，直接返回其 run_id，不重复构建
            recordTrigger(body, TriggerResultEnum.MERGED, null, existing.getId());
            return buildResult(TriggerResultEnum.MERGED, existing.getId(), null, null);
        }
        if (existing != null && RunStatusEnum.SUCCESS.getCode().equals(existing.getStatus())
                && !Boolean.TRUE.equals(body.getReuse())) {
            // 命中成功运行：先由用户确认是否复用制品，确认后带 reuse 重新触发
            return buildResult(TriggerResultEnum.PENDING_REUSE, existing.getId(), existing.getId(), null);
        }

        Long runId = createRun(body, pipeline, items, commit, existing == null ? null : existing.getId());
        recordTrigger(body, existing == null ? TriggerResultEnum.CREATED : TriggerResultEnum.REUSED, null, runId);
        return buildResult(existing == null ? TriggerResultEnum.CREATED : TriggerResultEnum.REUSED, runId, null, null);
    }

    @Override
    public IPage<TriggerRecordVO> triggerPageList(RunTriggerRecordQuery body) {
        return baseMapper.selectTriggerPageList(Page.create(body), body);
    }

    @Override
    public IPage<RunVO> pageList(RunQuery body) {
        return baseMapper.selectPageList(Page.create(body), body);
    }

    @Override
    public RunDetailVO detail(Long id) {
        Run run = getById(id);
        if (ObjUtil.isEmpty(run)) {
            throw new ServiceException("运行不存在");
        }
        RunDetailVO vo = BeanUtil.copy(run, RunDetailVO.class);
        vo.setPrechecks(parsePrechecks(run.getPrecheckResult()));
        vo.setStages(collectStages(run));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long id) {
        Run run = getById(id);
        if (ObjUtil.isEmpty(run)) {
            throw new ServiceException("运行不存在");
        }
        if (!RunStatusEnum.QUEUED.getCode().equals(run.getStatus())) {
            throw new ServiceException("仅排队中的运行可以取消");
        }
        return lambdaUpdate()
                .eq(Run::getId, run.getId())
                .set(Run::getStatus, RunStatusEnum.CANCELED.getCode())
                .set(Run::getFinishedTime, new Date())
                .update();
    }

    /**
     * 五项预校验：仓库可达与令牌有效、分支存在、提交可达、定义匹配、幂等检查
     *
     * <p>单项失败不中断，逐条收集结论；外部平台调用按项隔离，失败只影响该项结论。
     *
     * @param body 触发参数
     * @return 预校验结论列表，按五项顺序排列
     */
    private List<PrecheckItemVO> collectPrechecks(RunTriggerDTO body) {
        List<PrecheckItemVO> items = new ArrayList<>();
        App app = appService.getById(body.getAppId());
        Pipeline pipeline = pipelineService.getById(body.getPipelineId());

        // 应用不存在或停用时，外部平台与幂等校验失去前提，后四项直接按跳过输出
        if (ObjUtil.isEmpty(app) || !IntConst.ONE.equals(app.getStatus())) {
            String reason = ObjUtil.isEmpty(app) ? "应用不存在" : "应用已停用，不能发起运行";
            items.add(item("REPO", "仓库可达与令牌有效", PrecheckConclusionEnum.FAIL, reason));
            items.add(item("BRANCH", "分支存在", PrecheckConclusionEnum.NA, reason));
            items.add(item("COMMIT", "提交可达", PrecheckConclusionEnum.NA, reason));
            items.add(definitionItem(pipeline));
            items.add(item("IDEMPOTENT", "幂等检查", PrecheckConclusionEnum.NA, reason));
            return items;
        }

        // ① 仓库可达与令牌有效：能查到分支即视为可达
        List<String> branches = null;
        try {
            if (ObjUtil.isEmpty(app.getRepoType()) || ObjUtil.isEmpty(app.getRepoUrl())) {
                throw new ServiceException("应用未配置代码源，请先在应用管理中补齐");
            }
            branches = CodePlatformUtil.listBranches(app.getRepoType(), app.getRepoUrl(), decryptToken(app));
            items.add(item("REPO", "仓库可达与令牌有效", PrecheckConclusionEnum.PASS, "仓库可访问，令牌有效"));
        } catch (ServiceException e) {
            items.add(item("REPO", "仓库可达与令牌有效", PrecheckConclusionEnum.FAIL, e.getMessage()));
        }

        // ② 分支存在：分支候选本身就来自分支列表，这里做同一口径的复核
        if (branches == null) {
            items.add(item("BRANCH", "分支存在", PrecheckConclusionEnum.NA, "仓库不可达，跳过校验"));
        } else if (branches.contains(body.getBranch())) {
            items.add(item("BRANCH", "分支存在", PrecheckConclusionEnum.PASS, "分支「" + body.getBranch() + "」存在"));
        } else {
            items.add(item("BRANCH", "分支存在", PrecheckConclusionEnum.FAIL, "分支「" + body.getBranch() + "」不存在"));
        }

        // ③ 提交可达（带出提交人与时间）；标签/引用同样按引用可达校验
        if (branches == null) {
            items.add(item("COMMIT", "提交可达", PrecheckConclusionEnum.NA, "仓库不可达，跳过校验"));
        } else if (IntConst.ONE.equals(body.getRefType())) {
            items.add(item("COMMIT", "提交可达", PrecheckConclusionEnum.PASS, "标签/引用「" + body.getCommitId() + "」存在"));
        } else if (!RegexUtil.isCommitId(body.getCommitId())) {
            items.add(item("COMMIT", "提交可达", PrecheckConclusionEnum.FAIL, "提交id 必须是 7-40 位十六进制"));
        } else {
            try {
                CommitBrief commit = CodePlatformUtil.getCommit(app.getRepoType(), app.getRepoUrl(),
                        body.getCommitId(), decryptToken(app));
                items.add(item("COMMIT", "提交可达", PrecheckConclusionEnum.PASS,
                        "提交人 " + commit.author() + "，提交时间 " + formatDate(commit.committedTime())));
            } catch (ServiceException e) {
                items.add(item("COMMIT", "提交可达", PrecheckConclusionEnum.FAIL,
                        e.getMessage() + "；若该提交确属分支「" + body.getBranch() + "」，请核对提交id"));
            }
        }

        // ④ 定义匹配
        items.add(definitionItem(pipeline));

        // ⑤ 幂等检查
        items.add(idempotentItem(findLatestRun(body, pipeline)));
        return items;
    }

    /**
     * 创建运行记录与阶段快照
     *
     * @param body        触发参数
     * @param pipeline    流水线
     * @param prechecks   预校验结论
     * @param commit      提交摘要，标签/引用触发时为空
     * @param reusedRunId 复用的源运行id，非复用为空
     * @return 运行id
     */
    private Long createRun(RunTriggerDTO body, Pipeline pipeline, List<PrecheckItemVO> prechecks,
                           CommitBrief commit, Long reusedRunId) {
        Date now = new Date();
        boolean reused = reusedRunId != null;

        Run run = new Run();
        run.setAppId(body.getAppId());
        run.setPipelineId(pipeline.getId());
        run.setPipelineVersionId(pipeline.getCurrentVersionId());
        run.setPipelineVersionNo(pipeline.getCurrentVersionNo());
        run.setTriggerType(IntConst.ONE);
        run.setBranch(body.getBranch());
        run.setRefType(body.getRefType());
        run.setCommitId(body.getCommitId());
        run.setCommitAuthor(commit == null ? null : commit.author());
        run.setCommitTime(commit == null ? null : commit.committedTime());
        run.setTriggerNote(body.getTriggerNote());
        run.setTriggerUserName(currentUserName());
        // 复用运行不重新构建，直接落在成功终态；新建运行进入排队等待执行引擎
        run.setStatus(reused ? RunStatusEnum.SUCCESS.getCode() : RunStatusEnum.QUEUED.getCode());
        run.setReusedRunId(reusedRunId);
        run.setPrecheckResult(JsonUtil.toJson(prechecks));
        // 开始时间指执行开始而非触发时间，排队中的运行尚未开始执行
        run.setStartedTime(reused ? now : null);
        run.setFinishedTime(reused ? now : null);
        save(run);

        // 阶段快照按流水线版本定义生成，只落类型与执行状态，名称与超时从快照读取
        PipelineVersion version = pipelineVersionService.getById(pipeline.getCurrentVersionId());
        PipelineDefinitionVO definition = version == null ? null
                : JsonUtil.toBean(version.getContent(), PipelineDefinitionVO.class);
        List<PipelineStageVO> stages = definition == null || definition.getStages() == null
                ? List.of() : definition.getStages();
        for (int i = 0; i < stages.size(); i++) {
            RunStage record = new RunStage();
            record.setRunId(run.getId());
            record.setStageType(stages.get(i).getType());
            record.setStageOrder(i + 1);
            record.setStatus(reused ? RunStageStatusEnum.SKIPPED.getCode() : RunStageStatusEnum.PENDING.getCode());
            record.setStartedTime(reused ? now : null);
            record.setFinishedTime(reused ? now : null);
            runStageMapper.insert(record);
        }
        return run.getId();
    }

    /**
     * 组装运行的阶段列表，阶段名称、超时与任务明细从不可变的版本快照带出
     *
     * @param run 运行
     * @return 阶段列表
     */
    private List<RunStageVO> collectStages(Run run) {
        PipelineVersion version = pipelineVersionService.getById(run.getPipelineVersionId());
        PipelineDefinitionVO definition = version == null ? null
                : JsonUtil.toBean(version.getContent(), PipelineDefinitionVO.class);
        List<PipelineStageVO> stages = definition == null || definition.getStages() == null
                ? List.of() : definition.getStages();

        List<RunStageVO> stageVos = new ArrayList<>();
        List<RunStage> records = runStageMapper.selectList(Wrappers.<RunStage>lambdaQuery()
                .eq(RunStage::getRunId, run.getId()).orderByAsc(RunStage::getStageOrder));
        for (RunStage record : records) {
            RunStageVO stageVo = BeanUtil.copy(record, RunStageVO.class);
            int index = record.getStageOrder() == null ? 0 : record.getStageOrder() - 1;
            if (index >= 0 && index < stages.size()) {
                PipelineStageVO snapshot = stages.get(index);
                stageVo.setStageName(snapshot.getName());
                stageVo.setTimeoutMinutes(snapshot.getTimeoutMinutes());
                stageVo.setTasks(snapshot.getTasks() == null ? List.of() : BeanUtil.copy(snapshot.getTasks(), RunTaskVO.class));
            } else {
                stageVo.setTasks(List.of());
            }
            stageVos.add(stageVo);
        }
        return stageVos;
    }

    /**
     * 幂等键（应用、commit、流水线版本）查询最新一次运行
     *
     * @param body     触发参数
     * @param pipeline 流水线，可能不存在
     * @return 幂等命中的最新运行，未命中返回 null
     */
    private Run findLatestRun(RunTriggerDTO body, Pipeline pipeline) {
        if (pipeline == null || ObjUtil.isEmpty(pipeline.getCurrentVersionId())) {
            return null;
        }
        List<Run> hits = list(Wrappers.<Run>lambdaQuery()
                .eq(Run::getAppId, body.getAppId())
                .eq(Run::getCommitId, body.getCommitId())
                .eq(Run::getPipelineVersionId, pipeline.getCurrentVersionId())
                .orderByDesc(Run::getCreateTime));
        return hits.isEmpty() ? null : hits.get(0);
    }

    /**
     * 触发频率管控：同一用户对同一应用 1 分钟内超过 5 次直接拒绝
     *
     * @param appId 应用id
     */
    private void rateLimit(Long appId) {
        AuthUser user = AuthUtil.getUser();
        if (ObjUtil.isEmpty(user)) {
            return;
        }
        String key = "run:rate:" + user.getId() + ":" + appId;
        Long count = RedisUtil.incr(key, 1);
        if (count != null && count == 1) {
            RedisUtil.expire(key, RATE_LIMIT_SECONDS);
        }
        if (count != null && count > TRIGGER_RATE_LIMIT) {
            throw new ServiceException("操作过于频繁，请稍后再试");
        }
    }

    /**
     * 落触发记录
     *
     * @param body   触发参数
     * @param result 触发结果
     * @param reason 拒绝原因，非拒绝为空
     * @param runId  关联的运行id
     */
    private void recordTrigger(RunTriggerDTO body, TriggerResultEnum result, String reason, Long runId) {
        RunTrigger record = new RunTrigger();
        record.setAppId(body.getAppId());
        record.setBranch(body.getBranch());
        record.setCommitId(body.getCommitId());
        record.setResult(result.getCode());
        record.setRejectReason(reason);
        record.setRunId(runId);
        record.setTriggerUserName(currentUserName());
        runTriggerMapper.insert(record);
    }

    /**
     * 组装触发结果
     *
     * @param result              触发结果
     * @param runId               本次关联的运行id
     * @param reuseCandidateRunId 复用候选运行id
     * @param rejectReason        拒绝原因
     * @return 触发结果视图
     */
    private TriggerResultVO buildResult(TriggerResultEnum result, Long runId, Long reuseCandidateRunId, String rejectReason) {
        TriggerResultVO vo = new TriggerResultVO();
        vo.setResult(result.getCode());
        vo.setResultName(result.getDesc());
        vo.setRunId(runId);
        vo.setReuseCandidateRunId(reuseCandidateRunId);
        vo.setRejectReason(rejectReason);
        return vo;
    }

    /**
     * 定义匹配校验项：流水线必须存在且已配置定义；分支级门禁规则未启用时输出不适用
     *
     * @param pipeline 流水线，可能不存在
     * @return 预校验结论项
     */
    private PrecheckItemVO definitionItem(Pipeline pipeline) {
        if (ObjUtil.isEmpty(pipeline)) {
            return item("DEFINITION", "流水线定义匹配", PrecheckConclusionEnum.FAIL, "流水线不存在");
        }
        if (ObjUtil.isEmpty(pipeline.getCurrentVersionId())) {
            return item("DEFINITION", "流水线定义匹配", PrecheckConclusionEnum.FAIL, "流水线未配置定义");
        }
        return item("DEFINITION", "流水线定义匹配", PrecheckConclusionEnum.NA, "分支级门禁规则未启用，视为放行");
    }

    /**
     * 幂等检查项
     *
     * @param existing 幂等命中的最新运行，未命中为 null
     * @return 预校验结论项
     */
    private PrecheckItemVO idempotentItem(Run existing) {
        String detail;
        if (existing != null && ACTIVE_STATUSES.contains(existing.getStatus())) {
            detail = "存在进行中的运行，本次将合并到既有运行";
        } else if (existing != null) {
            detail = "存在成功运行，可选择复用其制品";
        } else {
            detail = "未命中既有运行，将创建新运行";
        }
        return item("IDEMPOTENT", "幂等检查", PrecheckConclusionEnum.PASS, detail);
    }

    /**
     * 解析随运行落库的预校验结论 JSON
     *
     * @param json 结论 JSON
     * @return 预校验结论列表
     */
    private List<PrecheckItemVO> parsePrechecks(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        return List.of(JsonUtil.toBean(json, PrecheckItemVO[].class));
    }

    /**
     * 解密应用登记的平台令牌
     *
     * @param app 应用
     * @return 明文令牌，只在内存中短暂使用，禁止落日志
     */
    private String decryptToken(App app) {
        if (StrUtil.isBlank(app.getRepoToken())) {
            throw new ServiceException("应用未配置平台访问令牌，请先在应用管理中补齐");
        }
        try {
            return CryptoUtil.aesDecrypt(app.getRepoToken(), securityProperties.getSecretKey());
        } catch (Exception e) {
            throw new ServiceException("平台访问令牌解密失败，请重新编辑应用");
        }
    }

    /**
     * 校验并加载可运行的应用（存在、启用）
     *
     * @param appId 应用id
     * @return 应用
     */
    private App loadApp(Long appId) {
        App app = appService.getById(appId);
        if (ObjUtil.isEmpty(app)) {
            throw new ServiceException("应用不存在");
        }
        if (!IntConst.ONE.equals(app.getStatus())) {
            throw new ServiceException("应用已停用");
        }
        return app;
    }

    /**
     * 构造预校验结论项
     *
     * @param item       预校验项标识
     * @param itemName   预校验项名称
     * @param conclusion 结论
     * @param detail     说明
     * @return 预校验结论项
     */
    private PrecheckItemVO item(String item, String itemName, PrecheckConclusionEnum conclusion, String detail) {
        PrecheckItemVO vo = new PrecheckItemVO();
        vo.setItem(item);
        vo.setItemName(itemName);
        vo.setConclusion(conclusion.getCode());
        vo.setDetail(detail);
        return vo;
    }

    /**
     * 格式化提交时间用于预校验说明
     *
     * @param time 提交时间
     * @return 格式化文本，为空返回占位
     */
    private String formatDate(Date time) {
        return time == null ? "—" : DateUtil.format(time, DateUtil.NORM_DATETIME_PATTERN);
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
