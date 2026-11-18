package com.ferry.devops.biz.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.GateExemptionDTO;
import com.ferry.devops.api.dto.query.GateExemptionQuery;
import com.ferry.devops.api.entity.GateExemption;
import com.ferry.devops.api.vo.GateExemptionVO;
import com.ferry.devops.api.vo.GateResultVO;

/**
 * 门禁结果服务：结果展示 + 豁免判定。
 */
public interface IGateResultService extends IService<GateExemption> {

    /** 运行下结果列表，不通过与数据获取失败置顶 */
    List<GateResultVO> listByRun(Long runId);

    /** 申请豁免：仅不通过可豁免，FETCH_FAILED 拒绝 */
    boolean exempt(GateExemptionDTO body);

    /** 运行下豁免列表（含到期状态判断） */
    IPage<GateExemptionVO> exemptionList(GateExemptionQuery query);

    /** 汇总运行门禁结论：存在 FAIL/FETCH_FAILED 则阻塞 */
    boolean isRunBlocked(Long runId);

    /** 撤销豁免 */
    boolean revoke(Long id);
}
