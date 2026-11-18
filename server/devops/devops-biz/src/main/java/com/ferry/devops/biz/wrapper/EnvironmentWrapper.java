package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.EnvironmentQuery;
import com.ferry.devops.api.entity.Environment;
import com.ferry.devops.api.vo.EnvironmentVO;

/**
 * 环境列表查询与VO转换
 */
public class EnvironmentWrapper extends BaseEntityWrapper<Environment, EnvironmentVO> {

    public static EnvironmentWrapper build() {
        return new EnvironmentWrapper();
    }

    @Override
    public EnvironmentVO vo(Environment entity) {
        return BeanUtil.copy(entity, EnvironmentVO.class);
    }

    /**
     * 按环境列表筛选条件构造查询
     *
     * @param query 查询条件
     * @return 查询包装器
     */
    public LambdaQueryWrapper<Environment> lambda(EnvironmentQuery query) {
        return Wrappers.<Environment>lambdaQuery()
                .eq(ObjUtil.isNotEmpty(query.getAppId()), Environment::getAppId, query.getAppId())
                .like(StrUtil.isNotBlank(query.getName()), Environment::getName, query.getName())
                .eq(ObjUtil.isNotEmpty(query.getCategory()), Environment::getCategory, query.getCategory())
                .eq(ObjUtil.isNotEmpty(query.getDeployType()), Environment::getDeployType, query.getDeployType())
                .eq(ObjUtil.isNotEmpty(query.getStatus()), Environment::getStatus, query.getStatus())
                .orderByDesc(Environment::getCreateTime);
    }
}
