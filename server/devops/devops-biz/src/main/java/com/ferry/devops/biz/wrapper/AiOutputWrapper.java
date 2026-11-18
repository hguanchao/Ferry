package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.AiOutputQuery;
import com.ferry.devops.api.entity.AiOutput;
import com.ferry.devops.api.vo.AiOutputVO;

/**
 * AI 输出查询与VO转换
 */
public class AiOutputWrapper extends BaseEntityWrapper<AiOutput, AiOutputVO> {

    public static AiOutputWrapper build() {
        return new AiOutputWrapper();
    }

    @Override
    public AiOutputVO vo(AiOutput entity) {
        return BeanUtil.copy(entity, AiOutputVO.class);
    }

    public LambdaQueryWrapper<AiOutput> lambda(AiOutputQuery query) {
        return Wrappers.<AiOutput>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getType()), AiOutput::getType, query.getType())
                .eq(StrUtil.isNotBlank(query.getBizId()), AiOutput::getBizId, query.getBizId())
                .eq(ObjUtil.isNotEmpty(query.getAppId()), AiOutput::getAppId, query.getAppId())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), AiOutput::getStatus, query.getStatus())
                .orderByDesc(AiOutput::getCreateTime);
    }
}
