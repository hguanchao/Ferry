package com.ferry.devops.biz.service.impl;

import java.util.Calendar;
import java.util.Date;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.security.utils.AuthUtil;
import com.ferry.devops.api.dto.GateExemptionDTO;
import com.ferry.devops.api.dto.query.GateExemptionQuery;
import com.ferry.devops.api.entity.GateExemption;
import com.ferry.devops.api.entity.GateRule;
import com.ferry.devops.api.entity.RunStageGate;
import com.ferry.devops.api.enums.GateStatusEnum;
import com.ferry.devops.api.vo.GateExemptionVO;
import com.ferry.devops.biz.mapper.GateExemptionMapper;
import com.ferry.devops.biz.mapper.GateRuleMapper;
import com.ferry.devops.biz.mapper.RunStageGateMapper;
import com.ferry.devops.biz.service.IGateExemptionService;
import com.ferry.devops.biz.service.IRunStageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 门禁豁免服务实现：申请、撤销、列表与到期处理。
 */
@Service
@RequiredArgsConstructor
public class GateExemptionServiceImpl extends ServiceImpl<GateExemptionMapper, GateExemption>
        implements IGateExemptionService {

    private final RunStageGateMapper runStageGateMapper;
    private final GateRuleMapper ruleMapper;
    private final IRunStageService runStageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean exempt(GateExemptionDTO body) {
        RunStageGate result = runStageGateMapper.selectById(body.getResultId());
        if (result == null) {
            throw new ServiceException("门禁结果不存在");
        }
        if (ObjUtil.equals(result.getStatus(), GateStatusEnum.FETCH_FAILED.getCode())) {
            throw new ServiceException("数据获取失败不可豁免");
        }
        if (!ObjUtil.equals(result.getStatus(), GateStatusEnum.FAIL.getCode())) {
            throw new ServiceException("仅不通过状态可申请豁免");
        }
        if (result.getRuleId() == null) {
            throw new ServiceException("门禁结果缺少规则信息，无法豁免");
        }
        if (StrUtil.isBlank(body.getReason()) || body.getReason().trim().length() < 20) {
            throw new ServiceException("豁免理由不少于 20 字");
        }
        if (body.getHours() == null || body.getHours() > 72) {
            throw new ServiceException("有效期不得超过 72 小时");
        }
        GateRule rule = ruleMapper.selectById(result.getRuleId());
        if (rule != null && ObjUtil.equals(rule.getIsExemptAllowed(), IntConst.ZERO)) {
            throw new ServiceException("该规则为安全红线，不允许豁免");
        }
        baseMapper.revokeActive(body.getResultId());
        GateExemption exemption = new GateExemption();
        exemption.setRunStageGateId(result.getId());
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
        runStageGateMapper.exempt(result.getId());
        runStageService.refreshAfterGateChange(result.getRunId(), result.getRunStageId());
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
                        .eq(ObjUtil.isNotEmpty(query.getRunStageGateId()),
                                GateExemption::getRunStageGateId, query.getRunStageGateId())
                        .eq(ObjUtil.isNotEmpty(query.getStatus()), GateExemption::getStatus, query.getStatus())
                        .orderByDesc(GateExemption::getCreateTime));
        // 到期判断在查询时进行：status=1 但已过期的置为 2，并恢复门禁状态
        for (GateExemption item : pages.getRecords()) {
            if (ObjUtil.equals(item.getStatus(), IntConst.ONE)
                    && item.getExpireAt() != null && item.getExpireAt().before(new Date())) {
                item.setStatus(IntConst.TWO);
                baseMapper.updateById(item);
                expireRunStageGate(item.getRunStageGateId());
            }
        }
        IPage<GateExemptionVO> result = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pages.getCurrent(), pages.getSize(), pages.getTotal());
        result.setRecords(BeanUtil.copy(pages.getRecords(), GateExemptionVO.class));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revoke(Long id) {
        GateExemption exemption = baseMapper.selectById(id);
        if (exemption == null) {
            throw new ServiceException("豁免记录不存在");
        }
        exemption.setStatus(IntConst.THREE);
        boolean updated = baseMapper.updateById(exemption) > IntConst.ZERO;
        if (updated && baseMapper.activeByRunStageGate(exemption.getRunStageGateId()) == null) {
            restoreFail(exemption.getRunStageGateId());
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireExemptions() {
        for (GateExemption exemption : baseMapper.selectExpiredActive(100)) {
            exemption.setStatus(IntConst.TWO);
            baseMapper.updateById(exemption);
            expireRunStageGate(exemption.getRunStageGateId());
        }
    }

    private void restoreFail(Long runStageGateId) {
        RunStageGate result = runStageGateMapper.selectById(runStageGateId);
        if (result == null) {
            return;
        }
        if (runStageGateMapper.updateStatus(runStageGateId, GateStatusEnum.EXEMPTED.getCode(),
                GateStatusEnum.FAIL.getCode()) > 0) {
            runStageService.refreshAfterGateChange(result.getRunId(), result.getRunStageId());
        }
    }

    private void expireRunStageGate(Long runStageGateId) {
        if (baseMapper.activeByRunStageGate(runStageGateId) == null) {
            restoreFail(runStageGateId);
        }
    }
}
