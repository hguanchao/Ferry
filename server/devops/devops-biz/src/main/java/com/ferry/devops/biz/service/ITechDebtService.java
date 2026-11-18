package com.ferry.devops.biz.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.FalsePositiveApplyDTO;
import com.ferry.devops.api.dto.TechDebtAssignDTO;
import com.ferry.devops.api.dto.query.TechDebtQuery;
import com.ferry.devops.api.entity.Issue;
import com.ferry.devops.api.vo.TechDebtVO;

/**
 * 技术债看板服务。
 */
public interface ITechDebtService extends IService<Issue> {

    IPage<TechDebtVO> pageList(TechDebtQuery query);

    boolean assign(TechDebtAssignDTO body);

    /** 误报申请：简化为直接改 issue.status=4 并写入基线排除集 */
    boolean applyFalsePositive(FalsePositiveApplyDTO body);

    /** 导出内容（CSV 文本） */
    String export(TechDebtQuery query);
}
