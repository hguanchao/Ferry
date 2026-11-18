package com.ferry.devops.biz.service.impl;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.devops.api.dto.FalsePositiveApplyDTO;
import com.ferry.devops.api.dto.TechDebtAssignDTO;
import com.ferry.devops.api.dto.query.TechDebtQuery;
import com.ferry.devops.api.entity.Issue;
import com.ferry.devops.api.vo.TechDebtVO;
import com.ferry.devops.biz.mapper.GateBaselineIssueMapper;
import com.ferry.devops.biz.mapper.IssueMapper;
import com.ferry.devops.biz.service.ITechDebtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 技术债看板服务实现：问题列表 + 指派 + 误报申请。
 * 简化实现：误报审批通过直接改 issue.status=4，并写入基线指纹排除集。
 */
@Service
@RequiredArgsConstructor
public class TechDebtServiceImpl extends ServiceImpl<IssueMapper, Issue> implements ITechDebtService {

    private final GateBaselineIssueMapper baselineIssueMapper;
    private final com.ferry.devops.biz.mapper.GateBaselineMapper baselineMapper;

    @Override
    public IPage<TechDebtVO> pageList(TechDebtQuery query) {
        IPage<Issue> pages = page(Page.create(query), Wrappers.<Issue>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getAppId()), Issue::getAppId, query.getAppId())
                .eq(ObjUtil.isNotEmpty(query.getSeverity()), Issue::getSeverity, query.getSeverity())
                .eq(ObjUtil.isNotEmpty(query.getRuleType()), Issue::getRuleType, query.getRuleType())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), Issue::getStatus, query.getStatus())
                .like(StrUtil.isNotBlank(query.getFingerprint()), Issue::getFingerprint, query.getFingerprint())
                .ge(StrUtil.isNotBlank(query.getFirstSeenStart()), Issue::getFirstSeen, query.getFirstSeenStart())
                .le(StrUtil.isNotBlank(query.getFirstSeenEnd()), Issue::getFirstSeen, query.getFirstSeenEnd())
                .orderByDesc(Issue::getFirstSeen));
        IPage<TechDebtVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        result.setRecords(BeanUtil.copy(pages.getRecords(), TechDebtVO.class));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assign(TechDebtAssignDTO body) {
        Issue issue = require(body.getId());
        issue.setAssignee(body.getAssigneeId());
        issue.setLastSeen(new Date());
        return updateById(issue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyFalsePositive(FalsePositiveApplyDTO body) {
        Issue issue = require(body.getId());
        if (StrUtil.isBlank(body.getReason()) || body.getReason().trim().length() < 5) {
            throw new ServiceException("误报理由过短");
        }
        issue.setStatus(IntConst.FOUR);
        issue.setLastSeen(new Date());
        updateById(issue);
        // 写入基线指纹排除集：当前生效基线中该指纹标记为误报
        com.ferry.devops.api.entity.GateBaseline baseline = baselineMapper.activeByApp(issue.getAppId());
        if (baseline != null) {
            com.ferry.devops.api.entity.GateBaselineIssue existing = baselineIssueMapper.selectOne(
                    Wrappers.<com.ferry.devops.api.entity.GateBaselineIssue>lambdaQuery()
                            .eq(com.ferry.devops.api.entity.GateBaselineIssue::getBaselineId, baseline.getId())
                            .eq(com.ferry.devops.api.entity.GateBaselineIssue::getFingerprint, issue.getFingerprint()));
            if (existing != null) {
                existing.setStatus(com.ferry.devops.api.enums.BaselineIssueStatusEnum.FALSE_POSITIVE.getCode());
                baselineIssueMapper.updateById(existing);
            }
        }
        return true;
    }

    @Override
    public String export(TechDebtQuery query) {
        List<Issue> issues = list(Wrappers.<Issue>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getAppId()), Issue::getAppId, query.getAppId())
                .eq(ObjUtil.isNotEmpty(query.getSeverity()), Issue::getSeverity, query.getSeverity())
                .eq(ObjUtil.isNotEmpty(query.getRuleType()), Issue::getRuleType, query.getRuleType())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), Issue::getStatus, query.getStatus())
                .orderByDesc(Issue::getFirstSeen));
        StringBuilder sb = new StringBuilder("id,appId,fingerprint,severity,ruleType,filePath,status,assignee,firstSeen\n");
        for (Issue issue : issues) {
            sb.append(issue.getId()).append(',')
                    .append(issue.getAppId()).append(',')
                    .append(issue.getFingerprint()).append(',')
                    .append(issue.getSeverity()).append(',')
                    .append(issue.getRuleType()).append(',')
                    .append(issue.getFilePath()).append(',')
                    .append(issue.getStatus()).append(',')
                    .append(issue.getAssignee()).append(',')
                    .append(issue.getFirstSeen()).append('\n');
        }
        return sb.toString();
    }

    private Issue require(Long id) {
        Issue issue = getById(id);
        if (issue == null) {
            throw new ServiceException("问题不存在");
        }
        return issue;
    }
}
