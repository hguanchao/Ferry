package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.GateExemptionDTO;
import com.ferry.devops.api.dto.query.GateExemptionQuery;
import com.ferry.devops.api.entity.GateExemption;
import com.ferry.devops.api.vo.GateExemptionVO;

/**
 * 门禁豁免服务：申请、撤销、列表与到期处理。
 */
public interface IGateExemptionService extends IService<GateExemption> {

    /** 申请豁免：仅不通过可豁免，数据获取失败拒绝 */
    boolean exempt(GateExemptionDTO body);

    /** 运行下豁免列表（含到期状态判断） */
    IPage<GateExemptionVO> exemptionList(GateExemptionQuery query);

    /** 撤销豁免 */
    boolean revoke(Long id);

    /** 处理已到期豁免：置为已到期，恢复门禁状态并刷新阶段/运行 */
    void expireExemptions();
}
