package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.DeployQuery;
import com.ferry.devops.api.entity.DeployRecord;
import com.ferry.devops.api.vo.DeployRecordVO;

/**
 * 部署记录列表查询与 VO 转换
 */
public class DeployWrapper extends BaseEntityWrapper<DeployRecord, DeployRecordVO> {

    public static DeployWrapper build() {
        return new DeployWrapper();
    }

    @Override
    public DeployRecordVO vo(DeployRecord entity) {
        return BeanUtil.copy(entity, DeployRecordVO.class);
    }

    /**
     * 部署记录查询条件
     */
    public LambdaQueryWrapper<DeployRecord> lambda(DeployQuery query) {
        return Wrappers.<DeployRecord>lambdaQuery()
                .like(StrUtil.isNotBlank(query.getDeployNo()), DeployRecord::getDeployNo, query.getDeployNo())
                .eq(ObjUtil.isNotEmpty(query.getAppId()), DeployRecord::getAppId, query.getAppId())
                .eq(ObjUtil.isNotEmpty(query.getEnvId()), DeployRecord::getEnvId, query.getEnvId())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), DeployRecord::getStatus, query.getStatus())
                .eq(ObjUtil.isNotEmpty(query.getDeployType()), DeployRecord::getDeployType, query.getDeployType())
                .ge(StrUtil.isNotBlank(query.getStartTimeStart()), DeployRecord::getStartTime, query.getStartTimeStart())
                .le(StrUtil.isNotBlank(query.getStartTimeEnd()), DeployRecord::getStartTime, query.getStartTimeEnd())
                .orderByDesc(DeployRecord::getCreateTime);
    }
}
