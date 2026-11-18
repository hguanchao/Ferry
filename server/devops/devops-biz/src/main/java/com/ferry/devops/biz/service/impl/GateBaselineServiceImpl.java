package com.ferry.devops.biz.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.dto.GateBaselineResetDTO;
import com.ferry.devops.api.entity.GateBaseline;
import com.ferry.devops.api.entity.GateBaselineReset;
import com.ferry.devops.api.vo.BaselineVO;
import com.ferry.devops.biz.mapper.GateBaselineIssueMapper;
import com.ferry.devops.biz.mapper.GateBaselineMapper;
import com.ferry.devops.biz.mapper.GateBaselineResetMapper;
import com.ferry.devops.biz.service.IGateBaselineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 存量基线服务实现。首次扫描生成快照(fingerprint)，只把不在基线指纹的 issue 计新增；
 * 重设/误报需审批（写 BaselineReset 表，approve 后生效）。
 */
@Service
@RequiredArgsConstructor
public class GateBaselineServiceImpl extends ServiceImpl<GateBaselineMapper, GateBaseline>
        implements IGateBaselineService {

    private final GateBaselineIssueMapper baselineIssueMapper;
    private final GateBaselineResetMapper resetMapper;

    @Override
    public BaselineVO detail(Long appId) {
        GateBaseline baseline = baseMapper.activeByApp(appId);
        if (baseline == null) {
            return null;
        }
        BaselineVO vo = new BaselineVO();
        vo.setId(baseline.getId());
        vo.setAppId(baseline.getAppId());
        vo.setTool(baseline.getTool());
        vo.setRuleVersion(baseline.getRuleVersion());
        vo.setTotalCount(baseline.getTotalCount());
        vo.setSeverityStats(baseline.getSeverityStats());
        vo.setFingerprintVersion(baseline.getFingerprintVersion());
        vo.setIsActive(baseline.getIsActive());
        vo.setCreateTime(baseline.getCreateTime());

        // 状态分布：1 遗留 2 已修复 3 误报
        for (Map<String, Object> row : baselineIssueMapper.statusStats(baseline.getId())) {
            Integer status = toInt(row.get("status"));
            Integer cnt = toInt(row.get("cnt"));
            if (ObjUtil.equals(status, IntConst.ONE)) {
                vo.setLegacyCount(cnt);
            } else if (ObjUtil.equals(status, IntConst.TWO)) {
                vo.setFixedCount(cnt);
            }
        }
        // 新增数 = 总数 - 基线遗留数（简化口径）
        vo.setNewCount(Math.max(baseline.getTotalCount() == null ? 0 : baseline.getTotalCount()
                - (vo.getLegacyCount() == null ? 0 : vo.getLegacyCount()), 0));

        GateBaselineReset pending = resetMapper.pending(appId);
        List<Long> pendingIds = new ArrayList<>();
        if (pending != null) {
            pendingIds.add(pending.getId());
        }
        vo.setPendingResetIds(pendingIds);
        vo.setDaysSinceReset(daysSince(baseline.getCreateTime()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reset(GateBaselineResetDTO body) {
        GateBaseline active = baseMapper.activeByApp(body.getAppId());
        GateBaselineReset apply = new GateBaselineReset();
        apply.setAppId(body.getAppId());
        apply.setOldBaselineId(active == null ? null : active.getId());
        apply.setReason(body.getReason());
        apply.setApprover(body.getApproverId());
        apply.setStatus(IntConst.ONE);
        apply.setCreateTime(new Date());
        return resetMapper.insert(apply) > IntConst.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long applyId, Boolean pass, String opinion) {
        GateBaselineReset apply = resetMapper.selectById(applyId);
        if (apply == null || !ObjUtil.equals(apply.getStatus(), IntConst.ONE)) {
            throw new ServiceException("申请不存在或已处理");
        }
        apply.setStatus(Boolean.TRUE.equals(pass) ? IntConst.TWO : IntConst.THREE);
        resetMapper.updateById(apply);
        if (Boolean.TRUE.equals(pass)) {
            // 审批通过：旧基线与新基线按当次扫描快照切换（最小实现：以请求中已落库的新基线为准）
            baseMapper.deactivateAll(apply.getAppId());
            if (ObjUtil.isNotEmpty(apply.getNewBaselineId())) {
                GateBaseline fresh = getById(apply.getNewBaselineId());
                if (fresh != null) {
                    fresh.setIsActive(IntConst.ONE);
                    updateById(fresh);
                }
            } else {
                // 无显式新基线时，将最近一条置为生效
                GateBaseline latest = list(Wrappers.<GateBaseline>lambdaQuery()
                        .eq(GateBaseline::getAppId, apply.getAppId()).orderByDesc(GateBaseline::getCreateTime)).stream()
                        .findFirst().orElse(null);
                if (latest != null) {
                    latest.setIsActive(IntConst.ONE);
                    updateById(latest);
                }
            }
        }
        return true;
    }

    @Override
    public boolean isNewIssue(Long appId, String fingerprint) {
        GateBaseline baseline = baseMapper.activeByApp(appId);
        if (baseline == null) {
            return true;
        }
        List<String> fingerprints = baselineIssueMapper.fingerprints(baseline.getId());
        Set<String> set = new HashSet<>(fingerprints);
        return !set.contains(fingerprint);
    }

    /**
     * 首次扫描生成基线快照与指纹集合。供静态扫描完成后调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public GateBaseline createSnapshot(Long appId, String tool, String ruleVersion, int totalCount,
                                       List<String> fingerprints) {
        GateBaseline baseline = new GateBaseline();
        baseline.setAppId(appId);
        baseline.setTool(tool);
        baseline.setRuleVersion(ruleVersion);
        baseline.setTotalCount(totalCount);
        baseline.setFingerprintVersion("v1");
        baseline.setIsActive(IntConst.ONE);
        baseline.setCreateTime(new Date());
        save(baseline);
        for (String fp : fingerprints) {
            com.ferry.devops.api.entity.GateBaselineIssue issue = new com.ferry.devops.api.entity.GateBaselineIssue();
            issue.setBaselineId(baseline.getId());
            issue.setFingerprint(fp);
            issue.setRuleId("");
            issue.setSeverity(IntConst.ONE);
            issue.setStatus(IntConst.ONE);
            baselineIssueMapper.insert(issue);
        }
        return baseline;
    }

    private Integer daysSince(Date time) {
        if (time == null) {
            return null;
        }
        return (int) ((System.currentTimeMillis() - time.getTime()) / (24L * 60 * 60 * 1000));
    }

    private Integer toInt(Object value) {
        return value == null ? null : Integer.valueOf(String.valueOf(value));
    }
}
