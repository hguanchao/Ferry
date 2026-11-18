package com.ferry.devops.biz.wrapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.BeanUtil;
import com.ferry.common.core.utils.CollUtil;
import com.ferry.common.core.utils.ObjUtil;
import com.ferry.common.core.utils.StrUtil;
import com.ferry.common.jdbc.base.BaseEntityWrapper;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.vo.AppVO;

/**
 * 应用列表查询与VO转换
 */
public class AppWrapper extends BaseEntityWrapper<App, AppVO> {

    public static AppWrapper build() {
        return new AppWrapper();
    }

    @Override
    public AppVO vo(App entity) {
        return BeanUtil.copy(entity, AppVO.class);
    }

    /**
     * 按应用列表筛选条件构造查询
     *
     * @param query 查询条件
     * @return 查询包装器
     */
    public LambdaQueryWrapper<App> lambda(AppQuery query) {
        return Wrappers.<App>lambdaQuery()
                .like(StrUtil.isNotBlank(query.getCode()), App::getCode, query.getCode())
                .like(StrUtil.isNotBlank(query.getName()), App::getName, query.getName())
                .in(CollUtil.isNotEmpty(query.getTeamIds()), App::getTeamId, query.getTeamIds())
                .in(CollUtil.isNotEmpty(query.getPlatforms()), App::getPlatform, query.getPlatforms())
                .eq(ObjUtil.isNotEmpty(query.getCoreFlag()), App::getIsCore, query.getCoreFlag())
                .orderByDesc(App::getCreateTime);
    }
}
