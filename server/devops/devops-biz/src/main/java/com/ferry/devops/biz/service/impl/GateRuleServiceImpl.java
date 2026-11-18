package com.ferry.devops.biz.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.constant.IntConst;
import com.ferry.common.core.exception.ServiceException;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.common.jdbc.provider.Page;
import com.ferry.devops.api.dto.GateRuleDTO;
import com.ferry.devops.api.dto.query.GateRuleQuery;
import com.ferry.devops.api.entity.GateRule;
import com.ferry.devops.api.enums.GateScopeEnum;
import com.ferry.devops.api.enums.RuleTypeEnum;
import com.ferry.devops.api.vo.GateRuleVO;
import com.ferry.devops.biz.mapper.GateRuleMapper;
import com.ferry.devops.biz.service.IGateRuleService;
import com.ferry.devops.biz.wrapper.GateRuleWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 门禁规则服务实现。阈值按 6 维度校验；组织级应用id写 0；应用/分支级必填应用id；
 * 分支级必填分支通配；修改规则版本 +1，且只影响新运行。
 */
@Service
@RequiredArgsConstructor
public class GateRuleServiceImpl extends ServiceImpl<GateRuleMapper, GateRule> implements IGateRuleService {

    @Override
    public IPage<GateRuleVO> pageList(GateRuleQuery body) {
        GateRuleWrapper wrapper = GateRuleWrapper.build();
        return wrapper.pageVo(page(Page.create(body), wrapper.lambda(body)));
    }

    @Override
    public GateRuleVO detail(Long id) {
        GateRule rule = getById(id);
        if (rule == null) {
            throw new ServiceException("门禁规则不存在");
        }
        return GateRuleWrapper.build().vo(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(GateRuleDTO body) {
        checkThreshold(body.getRuleType(), body.getThreshold());
        GateScopeEnum scope = GateScopeEnum.of(body.getScopeType());
        if (scope == null) {
            throw new ServiceException("作用域取值不合法");
        }
        if (ObjUtil.equals(body.getScopeType(), GateScopeEnum.ORGANIZATION.getCode())) {
            body.setAppId(0L);
            body.setBranchPattern(null);
        } else if (ObjUtil.isEmpty(body.getAppId())) {
            throw new ServiceException("应用级/分支级规则必须选择应用");
        }
        if (ObjUtil.equals(body.getScopeType(), GateScopeEnum.BRANCH.getCode())
                && StrUtil.isBlank(body.getBranchPattern())) {
            throw new ServiceException("分支级规则必须填写分支通配");
        }
        if (ObjUtil.isEmpty(body.getIsExemptAllowed())) {
            body.setIsExemptAllowed(IntConst.ONE);
        }
        if (ObjUtil.isEmpty(body.getIsEnabled())) {
            body.setIsEnabled(IntConst.ONE);
        }
        GateRule rule = new GateRule();
        rule.setId(body.getId());
        rule.setScopeType(body.getScopeType());
        rule.setAppId(body.getAppId());
        rule.setBranchPattern(body.getBranchPattern());
        rule.setRuleType(body.getRuleType());
        rule.setRuleName(body.getRuleName());
        rule.setThreshold(body.getThreshold());
        rule.setCompareDirection(body.getCompareDirection());
        rule.setStageTypes(body.getStageTypes());
        rule.setIsExemptAllowed(body.getIsExemptAllowed());
        rule.setIsEnabled(body.getIsEnabled());
        if (ObjUtil.isEmpty(body.getId())) {
            rule.setVersion(IntConst.ONE);
            return save(rule);
        }
        GateRule old = getById(body.getId());
        if (old == null) {
            throw new ServiceException("门禁规则不存在");
        }
        rule.setVersion((old.getVersion() == null ? IntConst.ONE : old.getVersion()) + IntConst.ONE);
        return updateById(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleted(List<Long> ids) {
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enable(Long id, Integer isEnabled) {
        GateRule rule = getById(id);
        if (rule == null) {
            throw new ServiceException("门禁规则不存在");
        }
        rule.setIsEnabled(isEnabled);
        rule.setVersion((rule.getVersion() == null ? IntConst.ONE : rule.getVersion()) + IntConst.ONE);
        return updateById(rule);
    }

    /**
     * 阈值校验：比率类 0-100 数值；数量类非负整数；黑名单类逗号分隔非空列表。
     */
    private void checkThreshold(Integer ruleType, String threshold) {
        RuleTypeEnum type = RuleTypeEnum.of(ruleType);
        if (type == null) {
            throw new ServiceException("规则类型取值不合法");
        }
        if (StrUtil.isBlank(threshold)) {
            throw new ServiceException("阈值不能为空");
        }
        if (type.isRatio()) {
            Double value = parseDouble(threshold);
            if (value < 0 || value > 100) {
                throw new ServiceException("比率类阈值应在 0-100 之间");
            }
        } else if (ObjUtil.equals(ruleType, RuleTypeEnum.LICENSE_BLACKLIST.getCode())) {
            String[] arr = threshold.split(",");
            for (String item : arr) {
                if (StrUtil.isBlank(item)) {
                    throw new ServiceException("黑名单阈值需为逗号分隔的非空列表");
                }
            }
        } else {
            Long value = parseLong(threshold);
            if (value < 0) {
                throw new ServiceException("数量类阈值应为非负整数");
            }
        }
    }

    private Double parseDouble(String value) {
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("阈值必须是数值");
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("阈值必须是整数");
        }
    }
}
