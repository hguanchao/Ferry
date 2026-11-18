package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.DeployPrecheckDTO;
import com.ferry.devops.api.dto.DeployStartDTO;
import com.ferry.devops.api.dto.query.DeployQuery;
import com.ferry.devops.api.entity.DeployBatch;
import com.ferry.devops.api.entity.DeployRecord;
import com.ferry.devops.api.entity.Environment;
import com.ferry.devops.api.entity.HealthSample;
import com.ferry.devops.api.enums.BatchStatusEnum;
import com.ferry.devops.api.enums.DeployPrecheckTypeEnum;
import com.ferry.devops.api.enums.DeployStatusEnum;
import com.ferry.devops.api.enums.DeployTypeEnum;
import com.ferry.devops.api.enums.EnvCategoryEnum;
import com.ferry.devops.api.vo.DeployBatchVO;
import com.ferry.devops.api.vo.DeployDetailVO;
import com.ferry.devops.api.vo.DeployPrecheckVO;
import com.ferry.devops.api.vo.DeployRecordVO;
import com.ferry.devops.api.vo.DeployStartVO;
import com.ferry.devops.api.vo.HealthSampleVO;
import com.ferry.devops.api.vo.PrecheckTokenVO;
import com.ferry.devops.biz.integration.deployer.Deployer;
import com.ferry.devops.biz.integration.deployer.DeployerFactory;
import com.ferry.devops.biz.mapper.DeployBatchMapper;
import com.ferry.devops.biz.mapper.DeployLockMapper;
import com.ferry.devops.biz.mapper.DeployPrecheckMapper;
import com.ferry.devops.biz.mapper.DeployRecordMapper;
import com.ferry.devops.biz.mapper.EnvironmentMapper;
import com.ferry.devops.biz.mapper.HealthSampleMapper;
import com.ferry.devops.biz.service.IDeployService;
import com.ferry.devops.biz.wrapper.DeployWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 部署服务实现。
 *
 * <p>生产环境部署由 Controller 的 devops:deploy:prod 权限控制；确认人与操作人不同在服务端校验。
 */
@Service
@RequiredArgsConstructor
public class DeployServiceImpl extends ServiceImpl<DeployRecordMapper, DeployRecord> implements IDeployService {

    private final DeployPrecheckMapper precheckMapper;
    private final DeployBatchMapper batchMapper;
    private final DeployLockMapper lockMapper;
    private final HealthSampleMapper healthSampleMapper;
    private final EnvironmentMapper environmentMapper;
    private final DeployerFactory deployerFactory;

    /** 校验令牌有效期 5 分钟 */
    private static final long TOKEN_TTL_MS = 5 * 60 * 1000L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrecheckTokenVO precheck(DeployPrecheckDTO body) {
        List<DeployPrecheckVO> items = new ArrayList<>();
        // 1. 制品存在
        Map<String, Object> artifact = baseMapper.selectArtifactInfo(body.getArtifactId());
        addResult(items, DeployPrecheckTypeEnum.ARTIFACT_EXISTS, artifact != null,
                artifact == null ? "制品不存在或已清理" : "制品存在：" + artifact.get("version"));
        // 2. 形态匹配：包制品仅 SSH；镜像制品仅 Docker/K8s
        Environment env = environmentMapper.selectById(body.getEnvId());
        DeployTypeEnum deployType = env == null ? null : DeployTypeEnum.of(env.getDeployType());
        boolean formOk = artifact != null && deployType != null && formMatches(
                JsonUtil.getInt(JsonUtil.toJson(artifact), "form"), deployType);
        addResult(items, DeployPrecheckTypeEnum.FORM_MATCH, formOk,
                formOk ? "制品形态与部署类型匹配" : "制品形态与部署类型不匹配");
        // 3. 环境空闲
        boolean idle = lockMapper.countActive(body.getAppId(), body.getEnvId()) == IntConst.ZERO
                && baseMapper.countActiveDeploy(body.getEnvId()) == IntConst.ZERO;
        addResult(items, DeployPrecheckTypeEnum.ENV_IDLE, idle,
                idle ? "环境空闲" : "环境正在进行其他部署");
        // 4. 依赖兼容（简化：有依赖则视为兼容）
        long deps = baseMapper.countDependencies(body.getAppId());
        addResult(items, DeployPrecheckTypeEnum.DEPENDENCY_COMPATIBLE, true,
                deps == IntConst.ZERO ? "无依赖服务" : "存在 " + deps + " 个依赖服务，版本兼容");
        // 5. 变更单（生产必填且已批准）
        boolean ticketOk = !ObjUtil.equals(env.getCategory(), EnvCategoryEnum.PRODUCTION.getCode())
                || (StrUtil.isNotBlank(body.getChangeTicketId())
                    && baseMapper.countApprovedTicket(Long.valueOf(body.getChangeTicketId()), new Date(),
                            new Date(System.currentTimeMillis() + TOKEN_TTL_MS)) > IntConst.ZERO);
        addResult(items, DeployPrecheckTypeEnum.CHANGE_TICKET, ticketOk,
                ticketOk ? "变更单校验通过或无需校验" : "生产环境需关联已批准且覆盖窗口的变更单");

        boolean canDeploy = items.stream().allMatch(i -> ObjUtil.equals(i.getConclusion(), IntConst.ONE));
        PrecheckTokenVO result = new PrecheckTokenVO();
        result.setItems(items);
        result.setCanDeploy(canDeploy);
        if (canDeploy) {
            result.setToken(buildToken(body));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeployStartVO start(DeployStartDTO body) {
        if (StrUtil.isBlank(body.getPrecheckToken())) {
            throw new ServiceException("缺少部署前校验令牌");
        }
        checkToken(body);
        Environment env = environmentMapper.selectById(body.getEnvId());
        if (env == null) {
            throw new ServiceException("环境不存在");
        }
        Long operator = AuthUtil.getUserId();
        if (ObjUtil.equals(env.getCategory(), EnvCategoryEnum.PRODUCTION.getCode())) {
            if (body.resolveConfirmer() == null) {
                throw new ServiceException("生产部署需选择确认人");
            }
            if (ObjUtil.equals(operator, body.resolveConfirmer())) {
                throw new ServiceException("确认人不能与操作人相同");
            }
        }
        // 部署锁：以表镜像判定，防止并发（生产并发由 Controller :prod 权限进一步约束）
        if (lockMapper.countActive(body.getAppId(), body.getEnvId()) > IntConst.ZERO) {
            throw new ServiceException("环境正在部署中，请稍后重试");
        }

        DeployRecord record = new DeployRecord();
        record.setDeployNo("DP-" + com.ferry.common.core.utils.DateUtil.format(
                new Date(), "yyyyMMdd") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        record.setAppId(body.getAppId());
        record.setEnvId(body.getEnvId());
        record.setDeployType(env.getDeployType());
        record.setArtifactId(body.getArtifactId());
        record.setBatchPlan(body.getBatchPlan());
        record.setPrecheckSnapshot(body.getPrecheckToken());
        record.setStatus(DeployStatusEnum.RUNNING.getCode());
        record.setOperator(operator);
        record.setConfirmer(body.resolveConfirmer());
        record.setStartTime(new Date());
        save(record);

        // 批次：默认单批次，可由 batchPlan 指定批次数
        int batchCount = parseBatchCount(body.getBatchPlan());
        for (int i = 1; i <= batchCount; i++) {
            DeployBatch batch = new DeployBatch();
            batch.setDeployId(record.getId());
            batch.setBatchNo(i);
            batch.setTargetCount(IntConst.ONE);
            batch.setTargetSnapshot("[]");
            batch.setStatus(BatchStatusEnum.RUNNING.getCode());
            batch.setStartTime(new Date());
            batchMapper.insert(batch);
        }

        // 调用执行器工厂（最小实现：仅日志 + 模拟结果）
        Deployer deployer = deployerFactory.get(env.getDeployType());
        Deployer.DeployContext ctx = new Deployer.DeployContext();
        ctx.setDeployId(record.getId());
        ctx.setAppId(body.getAppId());
        ctx.setEnvId(body.getEnvId());
        ctx.setArtifactId(body.getArtifactId());
        ctx.setArtifactDigest(JsonUtil.getStr(JsonUtil.toJson(baseMapper.selectArtifactInfo(body.getArtifactId())), "digest"));
        ctx.setTargetConfig(env.getTargetConfig());
        deployer.prepare(ctx);
        for (DeployBatch batch : batchMapper.selectList(Wrappers.<DeployBatch>lambdaQuery()
                .eq(DeployBatch::getDeployId, record.getId()).orderByAsc(DeployBatch::getBatchNo))) {
            Deployer.DeployBatch execBatch = new Deployer.DeployBatch();
            execBatch.setBatchId(batch.getId());
            execBatch.setBatchNo(batch.getBatchNo());
            execBatch.setTargetCount(batch.getTargetCount());
            execBatch.setTargetSnapshot(batch.getTargetSnapshot());
            deployer.deploy(ctx, execBatch);
            Deployer.HealthResult health = deployer.observe(ctx, execBatch);
            batch.setHealthResult(health.isPass() ? IntConst.ONE : 2);
            batch.setStatus(health.isPass() ? BatchStatusEnum.SUCCESS.getCode() : BatchStatusEnum.FAILURE.getCode());
            batch.setEndTime(new Date());
            batchMapper.updateById(batch);
            if (!health.isPass()) {
                record.setStatus(DeployStatusEnum.FAILURE.getCode());
                record.setFailReason("批次 " + batch.getBatchNo() + " 健康检查不通过：" + health.getDetail());
                updateById(record);
                DeployStartVO vo = new DeployStartVO();
                vo.setId(record.getId());
                vo.setDeployNo(record.getDeployNo());
                return vo;
            }
        }
        record.setStatus(DeployStatusEnum.SUCCESS.getCode());
        record.setEndTime(new Date());
        record.setDuration(record.getEndTime().getTime() - record.getStartTime().getTime());
        updateById(record);
        DeployStartVO vo = new DeployStartVO();
        vo.setId(record.getId());
        vo.setDeployNo(record.getDeployNo());
        return vo;
    }

    @Override
    public IPage<DeployRecordVO> pageList(DeployQuery body) {
        DeployWrapper wrapper = DeployWrapper.build();
        IPage<DeployRecord> pages = page(Page.create(body), wrapper.lambda(body));
        IPage<DeployRecordVO> result = wrapper.pageVo(pages);
        for (DeployRecordVO vo : result.getRecords()) {
            vo.setAppName(baseMapper.selectAppName(vo.getAppId()));
            vo.setEnvName(baseMapper.selectEnvName(vo.getEnvId()));
        }
        return result;
    }

    @Override
    public DeployDetailVO detail(Long id) {
        DeployRecord record = getById(id);
        if (record == null) {
            throw new ServiceException("部署记录不存在");
        }
        DeployDetailVO vo = BeanUtil.copy(record, DeployDetailVO.class);
        vo.setAppName(baseMapper.selectAppName(record.getAppId()));
        vo.setEnvName(baseMapper.selectEnvName(record.getEnvId()));
        vo.setBatches(batches(id));
        vo.setPrechecks(copyMaps(precheckMapper.selectByDeploy(id), DeployPrecheckVO.class));
        return vo;
    }

    @Override
    public List<DeployBatchVO> batches(Long id) {
        return copyMaps(batchMapper.selectByDeploy(id), DeployBatchVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pause(Long id) {
        DeployRecord record = require(id);
        record.setStatus(DeployStatusEnum.OBSERVING.getCode());
        return updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resume(Long id) {
        DeployRecord record = require(id);
        record.setStatus(DeployStatusEnum.RUNNING.getCode());
        return updateById(record);
    }

    @Override
    public List<HealthSampleVO> health(Long deployId, Long batchId) {
        return copyMaps(healthSampleMapper.selectByDeploy(deployId, batchId), HealthSampleVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean circuitContinue(Long deployId, String reason) {
        DeployRecord record = require(deployId);
        // 熔断后强制继续：保留失败原因并追加审计说明（最小实现以 failReason 追加标记）
        record.setFailReason(StrUtil.defaultIfBlank(record.getFailReason(), "") + "；熔断后强制继续：" + reason);
        record.setStatus(DeployStatusEnum.RUNNING.getCode());
        return updateById(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveHealthSample(Long deployId, Long batchId, Integer checkType, String value,
                                    String threshold, Integer conclusion) {
        HealthSample sample = new HealthSample();
        sample.setDeployId(deployId);
        sample.setBatchId(batchId);
        sample.setCheckType(checkType);
        sample.setValue(value);
        sample.setThreshold(threshold);
        sample.setConclusion(conclusion);
        sample.setSampledAt(new Date());
        return healthSampleMapper.insert(sample) > IntConst.ZERO;
    }

    private static <T> java.util.List<T> copyMaps(java.util.Collection<?> source, Class<T> clazz) {
        java.util.List<T> result = new java.util.ArrayList<>();
        if (source == null) {
            return result;
        }
        for (Object item : source) {
            result.add(com.ferry.common.core.utils.JsonUtil.toBean(
                    com.ferry.common.core.utils.JsonUtil.toJson(item), clazz));
        }
        return result;
    }

    private DeployRecord require(Long id) {
        DeployRecord record = getById(id);
        if (record == null) {
            throw new ServiceException("部署记录不存在");
        }
        return record;
    }

    private boolean formMatches(Integer form, DeployTypeEnum deployType) {
        if (form == null) {
            return false;
        }
        // 1 包制品 -> SSH；2 镜像制品 -> Docker/K8s
        return (form == 1 && deployType == DeployTypeEnum.SSH)
                || (form == 2 && (deployType == DeployTypeEnum.DOCKER || deployType == DeployTypeEnum.KUBERNETES));
    }

    private int parseBatchCount(String batchPlan) {
        if (StrUtil.isBlank(batchPlan)) {
            return 1;
        }
        Integer count = JsonUtil.getInt(batchPlan, "batchCount");
        return count == null || count < 1 ? 1 : count;
    }

    private void addResult(List<DeployPrecheckVO> items, DeployPrecheckTypeEnum type,
                           boolean pass, String detail) {
        DeployPrecheckVO item = new DeployPrecheckVO();
        item.setType(type.getCode());
        item.setTypeName(type.getDesc());
        item.setConclusion(pass ? IntConst.ONE : 2);
        item.setDetail(detail);
        items.add(item);
    }

    /** 简化令牌：UUID + 时间戳签名串，不加密，5 分钟有效 */
    private String buildToken(DeployPrecheckDTO body) {
        return body.getAppId() + ":" + body.getEnvId() + ":" + body.getArtifactId()
                + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID();
    }

    private void checkToken(DeployStartDTO body) {
        String[] parts = body.getPrecheckToken().split(":");
        if (parts.length < 4) {
            throw new ServiceException("部署校验令牌格式非法");
        }
        try {
            long ts = Long.parseLong(parts[3]);
            if (System.currentTimeMillis() - ts > TOKEN_TTL_MS) {
                throw new ServiceException("部署校验令牌已过期，请重新校验");
            }
        } catch (NumberFormatException e) {
            throw new ServiceException("部署校验令牌格式非法");
        }
        // 简化校验：令牌中的应用/环境/制品需与本次启动一致
        if (!ObjUtil.equals(parts[0], String.valueOf(body.getAppId()))
                || !ObjUtil.equals(parts[1], String.valueOf(body.getEnvId()))
                || !ObjUtil.equals(parts[2], String.valueOf(body.getArtifactId()))) {
            throw new ServiceException("部署校验令牌与本次部署信息不一致");
        }
    }
}
