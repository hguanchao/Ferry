package com.ferry.system.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.TenantDTO;
import com.ferry.system.api.dto.query.TenantQuery;
import com.ferry.system.api.entity.Tenant;
import com.ferry.system.api.vo.TenantVO;

import java.util.List;

public interface ITenantService extends IService<Tenant> {

    /**
     * 租户管理-列表
     *
     * @param body 查询条件
     * @return 租户列表
     */
    List<TenantVO> list(TenantQuery body);

    /**
     * 租户管理-分页列表
     *
     * @param body 查询条件
     * @return 分页列表
     */
    IPage<TenantVO> pageList(TenantQuery body);

    /**
     * 租户管理-编辑信息
     *
     * @param body 表单数据
     * @return 是否成功
     */
    boolean save(TenantDTO body);

    /**
     * 租户管理-删除信息
     *
     * @param ids 数据id，支持批量
     * @return 是否成功
     */
    boolean deleted(List<Long> ids);
}
