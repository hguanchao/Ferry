package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.GateRuleQuery;
import com.ferry.devops.api.entity.GateRule;
import com.ferry.devops.api.vo.GateRuleVO;

/**
 * 门禁规则查询与 VO 转换。
 */
public class GateRuleWrapper extends BaseEntityWrapper<GateRule, GateRuleVO> {

    public static GateRuleWrapper build() {
        return new GateRuleWrapper();
    }

    @Override
    public GateRuleVO vo(GateRule entity) {
        return BeanUtil.copy(entity, GateRuleVO.class);
    }

    public LambdaQueryWrapper<GateRule> lambda(GateRuleQuery query) {
        return Wrappers.<GateRule>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getScopeType()), GateRule::getScopeType, query.getScopeType())
                .eq(ObjUtil.isNotEmpty(query.getAppId()), GateRule::getAppId, query.getAppId())
                .eq(ObjUtil.isNotEmpty(query.getRuleType()), GateRule::getRuleType, query.getRuleType())
                .in(CollUtil.isNotEmpty(query.getRuleTypes()), GateRule::getRuleType, query.getRuleTypes())
                .like(StrUtil.isNotBlank(query.getRuleName()), GateRule::getRuleName, query.getRuleName())
                .eq(ObjUtil.isNotEmpty(query.getIsEnabled()), GateRule::getIsEnabled, query.getIsEnabled())
                .orderByDesc(GateRule::getCreateTime);
    }
}
