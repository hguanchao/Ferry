package com.ferry.system.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.ConfigDTO;
import com.ferry.system.api.dto.query.ConfigQuery;
import com.ferry.system.api.entity.Config;
import com.ferry.system.api.vo.ConfigVO;

import java.util.List;

public interface IConfigService extends IService<Config> {

    /**
     * 配置管理-分页列表
     *
     * @param body 查询条件
     * @return 分页列表
     */
    IPage<ConfigVO> pageList(ConfigQuery body);

    /**
     * 配置管理-编辑信息
     *
     * @param body 表单数据
     * @return 是否成功
     */
    boolean save(ConfigDTO body);

    /**
     * 配置管理-删除信息
     *
     * @param ids 数据id，支持批量
     * @return 是否成功
     */
    boolean deleted(List<Long> ids);
}
