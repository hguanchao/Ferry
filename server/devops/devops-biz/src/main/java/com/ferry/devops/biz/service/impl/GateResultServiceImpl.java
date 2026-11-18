package com.ferry.devops.biz.service.impl;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.GateExemptionDTO;
import com.ferry.devops.api.dto.query.GateExemptionQuery;
import com.ferry.devops.api.entity.GateExemption;
import com.ferry.devops.api.entity.GateResult;
import com.ferry.devops.api.entity.GateRule;
import com.ferry.devops.api.enums.GateConclusionEnum;
import com.ferry.devops.api.enums.GateScopeEnum;
import com.ferry.devops.api.vo.GateExemptionVO;
import com.ferry.devops.api.vo.GateResultVO;
import com.ferry.devops.biz.mapper.GateExemptionMapper;
import com.ferry.devops.biz.mapper.GateResultMapper;
import com.ferry.devops.biz.mapper.GateRuleMapper;
import com.ferry.devops.biz.service.IGateResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 门禁结果服务实现：结果汇总 + 规则优先级判定 + 豁免。
 *
 * <p>规则按 分支 > 应用 > 组织 优先级取最严格阈值；结论 PASS/FAIL/NOT_APPLICABLE/
 * EXEMPTED/FETCH_FAILED；汇总 run 下所有 gate_result，有 FAIL 或 FETCH_FAILED 则阻塞。
 */
@Service
@RequiredArgsConstructor
public class GateResultServiceImpl extends ServiceImpl<GateExemptionMapper, GateExemption>
        implements IGateResultService {

    private final GateResultMapper resultMapper;
    private final GateRuleMapper ruleMapper;

    @Override
    public List<GateResultVO> listByRun(Long runId) {
        return BeanUtil.copy(resultMapper.listByRun(runId), GateResultVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean exempt(GateExemptionDTO body) {
        GateResult result = resultMapper.selectById(body.getResultId());
        if (result == null) {
            throw new ServiceException("门禁结果不存在");
        }
        if (ObjUtil.equals(result.getConclusion(), GateConclusionEnum.FETCH_FAILED.getCode())) {
            throw new ServiceException("数据获取失败不可豁免");
        }
        if (!ObjUtil.equals(result.getConclusion(), GateConclusionEnum.FAIL.getCode())) {
            throw new ServiceException("仅不通过结论可申请豁免");
        }
        if (StrUtil.isBlank(body.getReason()) || body.getReason().trim().length() < 20) {
            throw new ServiceException("豁免理由不少于 20 字");
        }
        if (body.getHours() == null || body.getHours() > 72) {
            throw new ServiceException("有效期不得超过 72 小时");
        }
        // 规则是否允许豁免
        if (ObjUtil.isNotEmpty(result.getRuleId())) {
            GateRule rule = ruleMapper.selectById(result.getRuleId());
            if (rule != null && ObjUtil.equals(rule.getIsExemptAllowed(), IntConst.ZERO)) {
                throw new ServiceException("该规则为安全红线，不允许豁免");
            }
        }
        // 同一结果重复申请仅保留最后一次
        baseMapper.revokeActive(body.getResultId());
        GateExemption exemption = new GateExemption();
        exemption.setGateResultId(result.getId());
        exemption.setRunId(result.getRunId());
        exemption.setRuleId(result.getRuleId());
        exemption.setReason(body.getReason());
        exemption.setIssueNo(body.getIssueNo());
        exemption.setApplicant(AuthUtil.getUserId());
        exemption.setStatus(IntConst.ONE);
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR_OF_DAY, body.getHours());
        exemption.setExpireAt(cal.getTime());
        exemption.setCreateTime(new Date());
        baseMapper.insert(exemption);
        resultMapper.exempt(result.getId());
        return true;
    }

    @Override
    public IPage<GateExemptionVO> exemptionList(GateExemptionQuery query) {
        IPage<GateExemption> pages = baseMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        query.getCurrent() == null ? 1 : query.getCurrent(),
                        query.getSize() == null ? 30 : query.getSize()),
                Wrappers.<GateExemption>lambdaQuery()
                        .eq(ObjUtil.isNotEmpty(query.getRunId()), GateExemption::getRunId, query.getRunId())
                        .eq(ObjUtil.isNotEmpty(query.getGateResultId()), GateExemption::getGateResultId, query.getGateResultId())
                        .eq(ObjUtil.isNotEmpty(query.getStatus()), GateExemption::getStatus, query.getStatus())
                        .orderByDesc(GateExemption::getCreateTime));
        // 到期判断在查询时进行：status=1 但已过期的置为 2
        for (GateExemption item : pages.getRecords()) {
            if (ObjUtil.equals(item.getStatus(), IntConst.ONE)
                    && item.getExpireAt() != null && item.getExpireAt().before(new Date())) {
                item.setStatus(IntConst.TWO);
                baseMapper.updateById(item);
            }
        }
        IPage<GateExemptionVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        result.setRecords(BeanUtil.copy(pages.getRecords(), GateExemptionVO.class));
        return result;
    }

    @Override
    public boolean isRunBlocked(Long runId) {
        return resultMapper.countBlocking(runId) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revoke(Long id) {
        GateExemption exemption = baseMapper.selectById(id);
        if (exemption == null) {
            throw new ServiceException("豁免记录不存在");
        }
        exemption.setStatus(IntConst.THREE);
        return baseMapper.updateById(exemption) > IntConst.ZERO;
    }

    /**
     * 门禁判定：按 分支>应用>组织 加载规则，同作用域同类型取最严格阈值。
     * 比对当前值并写 gate_result；供运行判定流程调用（不在最小实现主链路内但保留真实逻辑）。
     *
     * @param runId      运行id
     * @param stageId    阶段id
     * @param appId      应用id
     * @param branch     分支
     * @param ruleType   规则类型
     * @param currentValue 当前值
     * @param source     报告来源
     * @param schemaVersion schema 版本
     * @return 写入的结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GateResult judge(Long runId, Long stageId, Long appId, String branch, Integer ruleType,
                            String currentValue, String source, String schemaVersion) {
        GateRule effective = pickStrictest(appId, branch, ruleType);
        GateResult result = new GateResult();
        result.setRunId(runId);
        result.setStageId(stageId);
        result.setRuleId(effective == null ? null : effective.getId());
        result.setRuleSnapshot(effective == null ? "{}" : com.ferry.common.core.utils.JsonUtil.toJson(effective));
        result.setThreshold(effective == null ? null : effective.getThreshold());
        result.setCurrentValue(currentValue);
        result.setSource(source);
        result.setSchemaVersion(schemaVersion);
        result.setCreateTime(new Date());
        if (effective == null || StrUtil.isBlank(currentValue)) {
            result.setConclusion(GateConclusionEnum.NOT_APPLICABLE.getCode());
        } else {
            boolean pass = compare(currentValue, effective.getThreshold(), effective.getCompareDirection(),
                    effective.getRuleType());
            result.setConclusion(pass ? GateConclusionEnum.PASS.getCode() : GateConclusionEnum.FAIL.getCode());
        }
        resultMapper.insert(result);
        return result;
    }

    /**
     * 数据获取失败判定：报告缺失/schema 不完整/拉取失败写 FETCH_FAILED(5)。
     *
     * <p>按规则优先级记录生效规则快照（不存在规则时记空快照）以便追溯；该结论不可豁免，
     * 且被 {@link #isRunBlocked(Long)} 计入阻塞汇总。
     *
     * @param runId         运行id
     * @param stageId       阶段id
     * @param appId         应用id
     * @param branch        分支
     * @param ruleType      规则类型
     * @param source        报告来源
     * @param schemaVersion schema 版本
     * @param fetchError    失败原因
     * @return 写入的结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GateResult judgeFetchFailed(Long runId, Long stageId, Long appId, String branch, Integer ruleType,
                                       String source, String schemaVersion, String fetchError) {
        GateRule effective = pickStrictest(appId, branch, ruleType);
        GateResult result = new GateResult();
        result.setRunId(runId);
        result.setStageId(stageId);
        result.setRuleId(effective == null ? null : effective.getId());
        result.setRuleSnapshot(effective == null ? "{}" : JsonUtil.toJson(effective));
        result.setThreshold(effective == null ? null : effective.getThreshold());
        result.setSource(source);
        result.setSchemaVersion(schemaVersion);
        result.setFetchError(fetchError);
        result.setConclusion(GateConclusionEnum.FETCH_FAILED.getCode());
        result.setCreateTime(new Date());
        resultMapper.insert(result);
        return result;
    }

    /**
     * 规则优先级：分支 > 应用 > 组织；同作用域同类型取最严格阈值。
     */
    private GateRule pickStrictest(Long appId, String branch, Integer ruleType) {
        Map<Integer, List<GateRule>> byScope = new HashMap<>();
        byScope.put(GateScopeEnum.BRANCH.getCode(), ruleMapper.branchRules(appId == null ? 0L : appId).stream()
                .filter(r -> ObjUtil.equals(r.getRuleType(), ruleType))
                .filter(r -> StrUtil.isBlank(r.getBranchPattern()) || branchMatch(r.getBranchPattern(), branch))
                .collect(java.util.stream.Collectors.toList()));
        byScope.put(GateScopeEnum.APPLICATION.getCode(),
                ruleMapper.byScope(GateScopeEnum.APPLICATION.getCode(), appId == null ? 0L : appId, ruleType));
        byScope.put(GateScopeEnum.ORGANIZATION.getCode(),
                ruleMapper.byScope(GateScopeEnum.ORGANIZATION.getCode(), 0L, ruleType));
        for (int priority : new int[]{GateScopeEnum.BRANCH.getPriority(), GateScopeEnum.APPLICATION.getPriority(),
                GateScopeEnum.ORGANIZATION.getPriority()}) {
            List<GateRule> rules = byScope.get(priority);
            if (CollUtil.isNotEmpty(rules)) {
                return strictest(rules);
            }
        }
        return null;
    }

    private GateRule strictest(List<GateRule> rules) {
        // 比率类「不低于」取阈值最大，「不高于」取最小；此处按数值极值为最严格。
        return rules.stream().max(Comparator.comparingDouble(r -> {
            try {
                return Double.parseDouble(r.getThreshold());
            } catch (Exception e) {
                return 0D;
            }
        })).orElse(rules.get(0));
    }

    private boolean branchMatch(String pattern, String branch) {
        if (StrUtil.isBlank(branch)) {
            return false;
        }
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return branch.matches(regex);
    }

    private boolean compare(String current, String threshold, Integer direction, Integer ruleType) {
        double cur = parse(current);
        double th = parse(threshold);
        int cmp = Double.compare(cur, th);
        // 不低于：当前值 >= 阈值；不高于：当前值 <= 阈值
        return ObjUtil.equals(direction, IntConst.TWO) ? cmp <= 0 : cmp >= 0;
    }

    private double parse(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0D;
        }
    }
}
