package com.ferry.devops.biz.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.GateRuleDTO;
import com.ferry.devops.api.dto.query.GateRuleQuery;
import com.ferry.devops.api.entity.GateRule;
import com.ferry.devops.api.vo.GateRuleVO;

/**
 * 门禁规则服务。
 */
public interface IGateRuleService extends IService<GateRule> {

    IPage<GateRuleVO> pageList(GateRuleQuery body);

    GateRuleVO detail(Long id);

    boolean save(GateRuleDTO body);

    boolean deleted(List<Long> ids);

    boolean enable(Long id, Integer isEnabled);
}
