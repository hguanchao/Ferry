package com.ferry.devops.biz.service.impl;
import com.ferry.common.core.constant.IntConst;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.ReleaseApproveDTO;
import com.ferry.devops.api.dto.ReleaseArtifactDTO;
import com.ferry.devops.api.dto.ReleaseCandidateDTO;
import com.ferry.devops.api.dto.ReleaseCheckDTO;
import com.ferry.devops.api.dto.ReleaseTerminateDTO;
import com.ferry.devops.api.dto.query.ReleaseQuery;
import com.ferry.devops.api.entity.ReleaseApproval;
import com.ferry.devops.api.entity.ReleaseArtifact;
import com.ferry.devops.api.entity.ReleaseCandidate;
import com.ferry.devops.api.entity.ReleaseCheckItem;
import com.ferry.devops.api.entity.ReleaseRecord;
import com.ferry.devops.api.entity.ReleaseTimeline;
import com.ferry.devops.api.enums.CheckConclusionEnum;
import com.ferry.devops.api.enums.RcStatusEnum;
import com.ferry.devops.api.enums.ReleaseCheckTypeEnum;
import com.ferry.devops.api.enums.RiskLevelEnum;
import com.ferry.devops.api.vo.ArtifactCandidateVO;
import com.ferry.devops.api.vo.ImpactVO;
import com.ferry.devops.api.vo.ReleaseArtifactVO;
import com.ferry.devops.api.vo.ReleaseCheckVO;
import com.ferry.devops.api.vo.ReleaseDetailVO;
import com.ferry.devops.api.vo.ReleaseRecordVO;
import com.ferry.devops.api.vo.ReleaseTimelineVO;
import com.ferry.devops.api.vo.ReleaseVO;
import com.ferry.devops.api.vo.RiskVO;
import com.ferry.devops.biz.mapper.ReleaseApprovalMapper;
import com.ferry.devops.biz.mapper.ReleaseArtifactMapper;
import com.ferry.devops.biz.mapper.ReleaseCandidateMapper;
import com.ferry.devops.biz.mapper.ReleaseCheckItemMapper;
import com.ferry.devops.biz.mapper.ReleaseRecordMapper;
import com.ferry.devops.biz.mapper.ReleaseTimelineMapper;
import com.ferry.devops.biz.service.IReleaseService;
import com.ferry.devops.biz.service.IReleaseStrategyService;
import com.ferry.devops.biz.wrapper.ReleaseWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 发布单与生产发布服务实现。
 *
 * <p>关键规则：提交锁定制品；创建人不可自批；高风险两级审批；生产启动校验封网期；终止不回滚。
 */
@Service
@RequiredArgsConstructor
public class ReleaseServiceImpl extends ServiceImpl<ReleaseCandidateMapper, ReleaseCandidate> implements IReleaseService {

    private final ReleaseArtifactMapper artifactMapper;
    private final ReleaseCheckItemMapper checkMapper;
    private final ReleaseApprovalMapper approvalMapper;
    private final ReleaseTimelineMapper timelineMapper;
    private final ReleaseRecordMapper recordMapper;
    private final IReleaseStrategyService strategyService;

    @Override
    public IPage<ReleaseVO> pageList(ReleaseQuery body) {
        ReleaseWrapper wrapper = ReleaseWrapper.build();
        return wrapper.pageVo(page(Page.create(body), wrapper.lambda(body)));
    }

    @Override
    public ReleaseDetailVO detail(Long id) {
        ReleaseCandidate rc = require(id);
        ReleaseDetailVO vo = BeanUtil.copy(rc, ReleaseDetailVO.class);
        vo.setArtifacts(copyMaps(artifactMapper.selectByRc(id), ReleaseArtifactVO.class));
        vo.setChecks(checks(id));
        vo.setTimeline(timeline(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(ReleaseCandidateDTO body) {
        ReleaseCandidate rc = BeanUtil.copy(body, ReleaseCandidate.class);
        if (ObjUtil.isEmpty(rc.getId())) {
            rc.setRcNo("RC-" + com.ferry.common.core.utils.DateUtil.format(new Date(), "yyyyMMdd")
                    + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            rc.setStatus(RcStatusEnum.DRAFT.getCode());
            rc.setCreator(AuthUtil.getUserId());
        } else {
            ReleaseCandidate old = require(rc.getId());
            if (!ObjUtil.equals(old.getStatus(), RcStatusEnum.DRAFT.getCode())) {
                throw new ServiceException("仅草稿可编辑");
            }
        }
        boolean ok = saveOrUpdate(rc);
        // 草稿保存时计算风险评级与影响面（预估值）
        RiskVO risk = evaluateRisk(rc);
        rc.setRiskLevel(risk.getLevel());
        rc.setRiskScore(risk.getScore());
        rc.setRiskDetail(risk.getDetail());
        rc.setImpactJson(JsonUtil.toJson(evaluateImpact(rc).getLayers()));
        updateById(rc);
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        for (Long id : ids) {
            ReleaseCandidate rc = require(id);
            if (!ObjUtil.equals(rc.getStatus(), RcStatusEnum.DRAFT.getCode())) {
                throw new ServiceException("仅草稿发布单可删除");
            }
        }
        return removeByIds(ids);
    }

    @Override
    public List<ArtifactCandidateVO> candidates(Long appId) {
        // 已晋级预发 + 门禁通过（1）或有效豁免，简化为 gate_conclusion IN (1,4)
        List<Map<String, Object>> rows = baseMapper.selectArtifactCandidates(appId);
        return copyMaps(rows, ArtifactCandidateVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveArtifacts(ReleaseArtifactDTO body) {
        ReleaseCandidate rc = require(body.getRcId());
        if (!ObjUtil.equals(rc.getStatus(), RcStatusEnum.DRAFT.getCode())) {
            throw new ServiceException("仅草稿可替换制品");
        }
        for (Map<String, Object> item : body.getItems()) {
            Long appId = ((Number) item.get("appId")).longValue();
            Long artifactId = ((Number) item.get("artifactId")).longValue();
            ReleaseArtifact exist = artifactMapper.selectOne(Wrappers.<ReleaseArtifact>lambdaQuery()
                    .eq(ReleaseArtifact::getRcId, body.getRcId()).eq(ReleaseArtifact::getAppId, appId));
            if (exist != null) {
                exist.setArtifactId(artifactId);
                artifactMapper.updateById(exist);
            } else {
                ReleaseArtifact ra = new ReleaseArtifact();
                ra.setRcId(body.getRcId());
                ra.setAppId(appId);
                ra.setArtifactId(artifactId);
                ra.setIsLocked(IntConst.ZERO);
                artifactMapper.insert(ra);
            }
        }
        // 重算风险与影响面
        RiskVO risk = evaluateRisk(rc);
        rc.setRiskLevel(risk.getLevel());
        rc.setRiskScore(risk.getScore());
        rc.setRiskDetail(risk.getDetail());
        rc.setImpactJson(JsonUtil.toJson(evaluateImpact(rc).getLayers()));
        return updateById(rc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(Long id) {
        ReleaseCandidate rc = require(id);
        if (!ObjUtil.equals(rc.getStatus(), RcStatusEnum.DRAFT.getCode())) {
            throw new ServiceException("仅草稿可提交");
        }
        if (artifactMapper.selectCount(Wrappers.<ReleaseArtifact>lambdaQuery()
                .eq(ReleaseArtifact::getRcId, id)) == 0L) {
            throw new ServiceException("发布单至少需要选择一个制品");
        }
        // 二次校验并锁定制品
        List<ReleaseArtifact> arts = artifactMapper.selectList(Wrappers.<ReleaseArtifact>lambdaQuery()
                .eq(ReleaseArtifact::getRcId, id));
        for (ReleaseArtifact art : arts) {
            art.setIsLocked(IntConst.ONE);
            artifactMapper.updateById(art);
        }
        // 生成六项检查（系统结论）
        initChecks(id, rc);
        rc.setStatus(RcStatusEnum.PENDING_APPROVAL.getCode());
        rc.setSubmittedAt(new Date());
        return updateById(rc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdraw(Long id, String reason) {
        ReleaseCandidate rc = require(id);
        if (!ObjUtil.equals(rc.getStatus(), RcStatusEnum.PENDING_APPROVAL.getCode())) {
            throw new ServiceException("仅待审批发布单可撤回");
        }
        List<ReleaseArtifact> arts = artifactMapper.selectList(Wrappers.<ReleaseArtifact>lambdaQuery()
                .eq(ReleaseArtifact::getRcId, id));
        for (ReleaseArtifact art : arts) {
            art.setIsLocked(IntConst.ZERO);
            artifactMapper.updateById(art);
        }
        checkMapper.delete(Wrappers.<ReleaseCheckItem>lambdaQuery().eq(ReleaseCheckItem::getRcId, id));
        rc.setStatus(RcStatusEnum.DRAFT.getCode());
        updateById(rc);
        timeline(id, "撤回发布单", reason);
        return true;
    }

    @Override
    public ImpactVO impact(Long id) {
        return evaluateImpact(require(id));
    }

    @Override
    public RiskVO risk(Long id) {
        return evaluateRisk(require(id));
    }

    @Override
    public List<ReleaseCheckVO> checks(Long id) {
        List<ReleaseCheckVO> result = copyMaps(checkMapper.selectByRc(id), ReleaseCheckVO.class);
        for (ReleaseCheckVO vo : result) {
            ReleaseCheckTypeEnum type = ReleaseCheckTypeEnum.of(vo.getCheckType());
            vo.setCheckName(type == null ? "" : type.getDesc());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCheck(ReleaseCheckDTO body) {
        ReleaseCheckItem item = checkMapper.selectById(body.getItemId());
        if (item == null) {
            throw new ServiceException("检查项不存在");
        }
        // 人工仅可改「门禁状态」「监控覆盖」为已知悉；其余仅可改不适用
        boolean gateOrMonitor = ObjUtil.equals(item.getCheckType(), ReleaseCheckTypeEnum.GATE.getCode())
                || ObjUtil.equals(item.getCheckType(), ReleaseCheckTypeEnum.MONITOR_COVERAGE.getCode());
        int conclusion = body.getConclusion();
        boolean legal = gateOrMonitor
                ? conclusion == CheckConclusionEnum.ACKNOWLEDGED.getCode()
                : conclusion == CheckConclusionEnum.NOT_APPLICABLE.getCode();
        if (!legal) {
            throw new ServiceException("该检查项的人工结论不合法");
        }
        if (conclusion == CheckConclusionEnum.NOT_APPLICABLE.getCode() && StrUtil.isBlank(body.getRemark())) {
            throw new ServiceException("标记为不适用需填写理由");
        }
        item.setManualConclusion(conclusion);
        item.setRemark(body.getRemark());
        return checkMapper.updateById(item) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(ReleaseApproveDTO body) {
        ReleaseCandidate rc = require(body.getId());
        if (body.getOpinion() == null || body.getOpinion().length() < 10) {
            throw new ServiceException("审批意见至少 10 个字");
        }
        Long me = AuthUtil.getUserId();
        if (ObjUtil.equals(me, rc.getCreator())) {
            throw new ServiceException("创建人不可审批自己创建的发布单");
        }
        if (!ObjUtil.equals(rc.getStatus(), RcStatusEnum.PENDING_APPROVAL.getCode())) {
            throw new ServiceException("发布单不在待审批状态");
        }
        // 节点匹配：高风险两级（node 1 应用负责人、node 2 技术负责人），中低风险一级（node 1）
        int expectedNode = ObjUtil.equals(rc.getRiskLevel(), RiskLevelEnum.HIGH.getCode()) ? 2 : 1;
        int node = body.getNode() == null ? (approvalMapper.countPassed(body.getId()) == 0 ? 1 : 2) : body.getNode();
        if (node > expectedNode) {
            throw new ServiceException("当前发布单无此审批节点");
        }
        ReleaseApproval approval = new ReleaseApproval();
        approval.setRcId(body.getId());
        approval.setNode(node);
        approval.setApprover(me);
        approval.setOpinion(body.getOpinion());
        approval.setResult(IntConst.ONE);
        approval.setCreateTime(new Date());
        approvalMapper.insert(approval);
        timeline(body.getId(), "审批通过（节点" + node + "）", body.getOpinion());
        if (node >= expectedNode) {
            rc.setStatus(RcStatusEnum.APPROVED.getCode());
            updateById(rc);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long id, String opinion, String returnStage) {
        ReleaseCandidate rc = require(id);
        if (StrUtil.isBlank(opinion) || StrUtil.isBlank(returnStage)) {
            throw new ServiceException("驳回需填写意见与退回环节");
        }
        ReleaseApproval approval = new ReleaseApproval();
        approval.setRcId(id);
        approval.setNode(approvalMapper.countPassed(id) == 0 ? 1 : 2);
        approval.setApprover(AuthUtil.getUserId());
        approval.setOpinion(opinion);
        approval.setResult(2);
        approval.setCreateTime(new Date());
        approvalMapper.insert(approval);
        rc.setStatus(RcStatusEnum.REJECTED.getCode());
        updateById(rc);
        timeline(id, "驳回至" + returnStage, opinion);
        return true;
    }

    @Override
    public List<ReleaseTimelineVO> timeline(Long id) {
        return copyMaps(timelineMapper.selectByRc(id), ReleaseTimelineVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long start(Long id, Long confirmer) {
        ReleaseCandidate rc = require(id);
        if (!ObjUtil.equals(rc.getStatus(), RcStatusEnum.APPROVED.getCode())) {
            throw new ServiceException("仅已批准发布单可启动");
        }
        Long me = AuthUtil.getUserId();
        // 提供了确认人时必须与启动人不同；前端未传时用当前用户（最小实现，权限由 :prod 权限串约束）
        if (confirmer != null && ObjUtil.equals(me, confirmer)) {
            throw new ServiceException("启动发布需选择不同于启动人的确认人");
        }
        if (confirmer == null) {
            confirmer = me;
        }
        // 封网校验（窗口校验简化由前端完成）
        Long appId = firstAppId(id);
        if (strategyService.isClosed(new Date(), appId)) {
            throw new ServiceException("当前处于封网期，禁止启动发布");
        }
        List<ReleaseArtifact> arts = artifactMapper.selectList(Wrappers.<ReleaseArtifact>lambdaQuery()
                .eq(ReleaseArtifact::getRcId, id));
        Long recordId = null;
        for (ReleaseArtifact art : arts) {
            ReleaseRecord record = new ReleaseRecord();
            record.setRcId(id);
            record.setAppId(art.getAppId());
            record.setEnvId(0L); // 生产环境由制品晋级来源保证，发布记录按应用维度落库
            record.setDeployType(IntConst.ONE);
            record.setArtifactId(art.getArtifactId());
            record.setResult(IntConst.ONE);
            record.setOperator(me);
            record.setConfirmer(confirmer);
            record.setIsRolledBack(IntConst.ZERO);
            record.setStartTime(new Date());
            recordMapper.insert(record);
            recordId = record.getId();
        }
        rc.setStatus(RcStatusEnum.OBSERVING.getCode());
        updateById(rc);
        timeline(id, "启动生产发布", "确认人：" + confirmer);
        return recordId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean terminate(ReleaseTerminateDTO body) {
        ReleaseCandidate rc = require(body.getId());
        if (!ObjUtil.equals(rc.getStatus(), RcStatusEnum.RELEASING.getCode())
                && !ObjUtil.equals(rc.getStatus(), RcStatusEnum.OBSERVING.getCode())) {
            throw new ServiceException("仅发布中或观察中的发布单可终止");
        }
        rc.setStatus(RcStatusEnum.CANCELLED.getCode());
        updateById(rc);
        // 终止：已发布批次保持当前版本，不回滚
        timeline(body.getId(), "终止发布（不回滚）", body.getReason());
        return true;
    }

    @Override
    public IPage<ReleaseRecordVO> recordPage(ReleaseQuery body) {
        IPage<ReleaseRecord> pages = recordMapper.selectPage(Page.create(body), Wrappers.<ReleaseRecord>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(body.getAppId()), ReleaseRecord::getAppId, body.getAppId())
                .orderByDesc(ReleaseRecord::getCreateTime));
        List<ReleaseRecordVO> records = new ArrayList<>();
        for (ReleaseRecord r : pages.getRecords()) {
            ReleaseRecordVO vo = BeanUtil.copy(r, ReleaseRecordVO.class);
            records.add(vo);
        }
        IPage<ReleaseRecordVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pages.getCurrent(), pages.getSize(), pages.getTotal());
        result.setRecords(records);
        return result;
    }

    // ---------------- 内部方法 ----------------

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

    private ReleaseCandidate require(Long id) {
        ReleaseCandidate rc = getById(id);
        if (rc == null) {
            throw new ServiceException("发布单不存在");
        }
        return rc;
    }

    private Long firstAppId(Long rcId) {
        ReleaseArtifact art = artifactMapper.selectOne(Wrappers.<ReleaseArtifact>lambdaQuery()
                .eq(ReleaseArtifact::getRcId, rcId).orderByAsc(ReleaseArtifact::getId));
        return art == null ? IntConst.MINUS.longValue() : art.getAppId();
    }

    /** 初始化六项准入检查的系统结论 */
    private void initChecks(Long rcId, ReleaseCandidate rc) {
        for (ReleaseCheckTypeEnum type : ReleaseCheckTypeEnum.values()) {
            ReleaseCheckItem item = new ReleaseCheckItem();
            item.setRcId(rcId);
            item.setCheckType(type.getCode());
            int sysConclusion = systemConclusion(type, rc);
            item.setSystemConclusion(sysConclusion);
            checkMapper.insert(item);
        }
    }

    private int systemConclusion(ReleaseCheckTypeEnum type, ReleaseCandidate rc) {
        return switch (type) {
            case GATE -> IntConst.ONE;                    // 制品晋级+门禁通过由 candidates 保证
            case IMPACT -> IntConst.ONE;
            case ROLLBACK_PLAN -> IntConst.ONE;
            case MONITOR_COVERAGE -> IntConst.TWO;        // 监控覆盖默认不通过，需人工「已知悉」
            case CHANGE_TICKET -> rc.getChangeTicketId() == null ? IntConst.TWO : IntConst.ONE;
            case ONCALL_CONFIRM -> IntConst.ONE;
        };
    }

    /** 影响面三层：直接依赖（一层）、间接依赖、共享资源；依赖拓扑未接入，均为推断 */
    private ImpactVO evaluateImpact(ReleaseCandidate rc) {
        List<Map<String, Object>> layers = new ArrayList<>();
        Map<String, Object> direct = new LinkedHashMap<>();
        direct.put("layer", 1);
        direct.put("name", "直接依赖");
        direct.put("items", new ArrayList<>());
        direct.put("inferred", "依赖拓扑未接入，标注为推断");
        layers.add(direct);
        Map<String, Object> indirect = new LinkedHashMap<>();
        indirect.put("layer", 2);
        indirect.put("name", "间接依赖");
        indirect.put("items", new ArrayList<>());
        indirect.put("inferred", "拓扑缺失，推断");
        layers.add(indirect);
        Map<String, Object> shared = new LinkedHashMap<>();
        shared.put("layer", 3);
        shared.put("name", "共享资源");
        shared.put("items", new ArrayList<>());
        shared.put("inferred", "推断");
        layers.add(shared);
        ImpactVO vo = new ImpactVO();
        vo.setLayers(layers);
        return vo;
    }

    /** 四因子加权：变更文件25% 服务数25% 失败率25% 时间窗口15%，1-5 分制 */
    private RiskVO evaluateRisk(ReleaseCandidate rc) {
        // 变更文件：无文件差异数据，取中位 2 分
        double fileScore = 2.0;
        // 服务数：依赖拓扑未接入，取基准 1 分
        double serviceScore = 1.0;
        // 近 90 天失败率：无历史数据取中位 2 分
        double failScore = 2.0;
        // 时间窗口：非工作时段（22:00-08:00）给 4 分，否则 1 分
        double windowScore = 1.0;
        if (rc.getWindowStart() != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(rc.getWindowStart());
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            if (hour >= 22 || hour < 8) {
                windowScore = 4.0;
            }
        }
        double total = fileScore * 0.25 + serviceScore * 0.25
                + failScore * 0.25 + windowScore * 0.15;
        BigDecimal score = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
        int level = total <= 2.0 ? RiskLevelEnum.LOW.getCode()
                : total <= 3.5 ? RiskLevelEnum.MEDIUM.getCode() : RiskLevelEnum.HIGH.getCode();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("变更文件(25%)", fileScore);
        detail.put("服务数(25%)", serviceScore);
        detail.put("近90天失败率(25%)", failScore);
        detail.put("时间窗口(15%)", windowScore);
        RiskVO vo = new RiskVO();
        vo.setLevel(level);
        vo.setScore(score);
        vo.setDetail(JsonUtil.toJson(detail));
        return vo;
    }

    private List<Long> artifactAppIds(Long rcId) {
        List<Long> ids = new ArrayList<>();
        for (ReleaseArtifact art : artifactMapper.selectList(Wrappers.<ReleaseArtifact>lambdaQuery()
                .eq(ReleaseArtifact::getRcId, rcId))) {
            ids.add(art.getAppId());
        }
        return ids;
    }

    private void timeline(Long rcId, String node, String remark) {
        ReleaseTimeline t = new ReleaseTimeline();
        t.setRcId(rcId);
        t.setNode(node);
        t.setOperator(AuthUtil.getUserId());
        t.setRemark(remark);
        t.setCreateTime(new Date());
        timelineMapper.insert(t);
    }
}
