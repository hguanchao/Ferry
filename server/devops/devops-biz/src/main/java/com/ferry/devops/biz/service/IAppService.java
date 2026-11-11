package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.AppDTO;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.vo.AppVO;

import java.util.List;

public interface IAppService extends IService<App> {

    /**
     * 应用管理-分页列表
     *
     * @param body 查询条件
     * @return 应用分页列表
     */
    IPage<AppVO> pageList(AppQuery body);

    /**
     * 应用管理-详情
     *
     * @param id 应用id
     * @return 应用详情
     */
    AppVO detail(Long id);

    /**
     * 应用管理-新增或编辑
     *
     * @param body 表单数据
     * @return true 成功 || false 失败
     */
    boolean save(AppDTO body);

    /**
     * 应用管理-逻辑删除
     *
     * @param ids 应用id集合
     * @return true 成功 || false 失败
     */
    boolean deleted(List<Long> ids);
}
