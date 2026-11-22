package com.ferry.devops.biz.service.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.entity.GateRule;
import com.ferry.devops.api.entity.RunStageGate;
import com.ferry.devops.api.enums.GateScopeEnum;
import com.ferry.devops.api.enums.GateStatusEnum;
import com.ferry.devops.api.vo.RunStageGateVO;
import com.ferry.devops.biz.mapper.GateRuleMapper;
import com.ferry.devops.biz.mapper.RunStageGateMapper;
import com.ferry.devops.biz.service.IRunStageGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 运行阶段门禁结果服务实现：结果汇总 + 规则优先级判定。
 *
 * <p>规则按 分支 > 应用 > 组织 优先级取最严格阈值，并按阶段类型过滤；
 * 状态枚举 PASS/FAIL/NOT_APPLICABLE/EXEMPTED/FETCH_FAILED，汇总运行下所有结果。
 */
@Service
@RequiredArgsConstructor
public class RunStageGateServiceImpl extends ServiceImpl<RunStageGateMapper, RunStageGate>
        implements IRunStageGateService {

    private final GateRuleMapper ruleMapper;

    @Override
    public List<RunStageGateVO> listByRun(Long runId) {
        return BeanUtil.copy(baseMapper.listByRun(runId), RunStageGateVO.class);
    }

    @Override
    public boolean isRunBlocked(Long runId) {
        return baseMapper.countBlocking(runId) > IntConst.ZERO;
    }

    @Override
    public boolean hasApplicableRule(Long appId, String branch, Integer stageType, Integer ruleType) {
        return pickStrictest(appId, branch, ruleType, stageType) != null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunStageGate judge(Long runId, Long runStageId, Long appId, String branch, Integer stageType,
                              Integer ruleType, String currentValue, String source, String schemaVersion) {
        GateRule effective = pickStrictest(appId, branch, ruleType, stageType);
        RunStageGate result = baseResult(runId, runStageId, ruleType, effective, source, schemaVersion);
        result.setCurrentValue(StrUtil.isBlank(currentValue) ? null : currentValue.trim());
        result.setFetchError(null);
        if (effective == null || StrUtil.isBlank(currentValue)) {
            result.setStatus(GateStatusEnum.NOT_APPLICABLE.getCode());
        } else {
            boolean pass = compare(currentValue, effective.getThreshold(), effective.getCompareDirection(),
                    effective.getRuleType());
            result.setStatus(pass ? GateStatusEnum.PASS.getCode() : GateStatusEnum.FAIL.getCode());
        }
        baseMapper.upsert(result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RunStageGate judgeFetchFailed(Long runId, Long runStageId, Long appId, String branch, Integer stageType,
                                         Integer ruleType, String source, String schemaVersion, String fetchError) {
        GateRule effective = pickStrictest(appId, branch, ruleType, stageType);
        RunStageGate result = baseResult(runId, runStageId, ruleType, effective, source, schemaVersion);
        result.setFetchError(fetchError);
        result.setStatus(GateStatusEnum.FETCH_FAILED.getCode());
        baseMapper.upsert(result);
        return result;
    }

    private RunStageGate baseResult(Long runId, Long runStageId, Integer ruleType, GateRule effective,
                                    String source, String schemaVersion) {
        RunStageGate result = new RunStageGate();
        result.setId(IdWorker.getId());
        result.setRunId(runId);
        result.setRunStageId(runStageId);
        result.setRuleType(ruleType);
        result.setRuleId(effective == null ? null : effective.getId());
        result.setRuleVersion(effective == null ? null : effective.getVersion());
        result.setRuleSnapshot(effective == null ? "{}" : JsonUtil.toJson(effective));
        result.setThreshold(effective == null ? null : effective.getThreshold());
        result.setSource(source);
        result.setEvidence(source);
        result.setSchemaVersion(schemaVersion);
        Date now = new Date();
        result.setCreateTime(now);
        result.setUpdateTime(now);
        return result;
    }

    /**
     * 规则优先级：分支 > 应用 > 组织；同作用域同类型取最严格阈值，并按阶段类型过滤。
     */
    private GateRule pickStrictest(Long appId, String branch, Integer ruleType, Integer stageType) {
        Map<Integer, List<GateRule>> byScope = new HashMap<>();
        byScope.put(GateScopeEnum.BRANCH.getCode(), ruleMapper.branchRules(appId == null ? 0L : appId).stream()
                .filter(r -> ObjUtil.equals(r.getRuleType(), ruleType))
                .filter(r -> stageTypesContains(r.getStageTypes(), stageType))
                .filter(r -> StrUtil.isBlank(r.getBranchPattern()) || branchMatch(r.getBranchPattern(), branch))
                .collect(Collectors.toList()));
        byScope.put(GateScopeEnum.APPLICATION.getCode(),
                ruleMapper.byScope(GateScopeEnum.APPLICATION.getCode(), appId == null ? 0L : appId, ruleType, stageType));
        byScope.put(GateScopeEnum.ORGANIZATION.getCode(),
                ruleMapper.byScope(GateScopeEnum.ORGANIZATION.getCode(), 0L, ruleType, stageType));
        for (int priority : new int[]{GateScopeEnum.BRANCH.getPriority(), GateScopeEnum.APPLICATION.getPriority(),
                GateScopeEnum.ORGANIZATION.getPriority()}) {
            List<GateRule> rules = byScope.get(priority);
            if (CollUtil.isNotEmpty(rules)) {
                return strictest(rules, ruleType);
            }
        }
        return null;
    }

    private GateRule strictest(List<GateRule> rules, Integer ruleType) {
        if (ObjUtil.equals(ruleType, 6)) {
            // 许可证黑名单：黑名单集合越大越严格
            return rules.stream()
                    .max(Comparator.comparingInt(rule -> blacklistSize(rule.getThreshold())))
                    .orElse(rules.get(0));
        }
        boolean allLowerBetter = rules.stream()
                .allMatch(rule -> ObjUtil.equals(rule.getCompareDirection(), IntConst.TWO));
        Comparator<GateRule> comparator = Comparator.comparingDouble(rule -> parse(rule.getThreshold()));
        return allLowerBetter
                ? rules.stream().min(comparator).orElse(rules.get(0))
                : rules.stream().max(comparator).orElse(rules.get(0));
    }

    private boolean stageTypesContains(String stageTypes, Integer stageType) {
        if (stageType == null || StrUtil.isBlank(stageTypes)) {
            return true;
        }
        Set<String> values = new HashSet<>(Arrays.asList(stageTypes.split(",")));
        return values.stream().map(String::trim).anyMatch(value -> value.equals(String.valueOf(stageType)));
    }

    private int blacklistSize(String threshold) {
        if (StrUtil.isBlank(threshold)) {
            return 0;
        }
        return threshold.split(",").length;
    }

    private boolean branchMatch(String pattern, String branch) {
        if (StrUtil.isBlank(branch)) {
            return false;
        }
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return branch.matches(regex);
    }

    private boolean compare(String current, String threshold, Integer direction, Integer ruleType) {
        if (ObjUtil.equals(ruleType, 6)) {
            return !licenseBlocked(current, threshold);
        }
        double cur = parse(current);
        double th = parse(threshold);
        int cmp = Double.compare(cur, th);
        // 不低于：当前值 >= 阈值；不高于：当前值 <= 阈值
        return ObjUtil.equals(direction, IntConst.TWO) ? cmp <= 0 : cmp >= 0;
    }

    /**
     * 许可证黑名单精确匹配：检测到的许可证与黑名单有交集即不通过。
     */
    private boolean licenseBlocked(String current, String threshold) {
        if (StrUtil.isBlank(current) || StrUtil.isBlank(threshold)) {
            return false;
        }
        Set<String> detected = Arrays.stream(current.split(","))
                .map(String::trim).filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        Set<String> blacklist = Arrays.stream(threshold.split(","))
                .map(String::trim).filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        return detected.stream().anyMatch(blacklist::contains);
    }

    private double parse(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return 0D;
        }
    }
}
