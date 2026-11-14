package com.ferry.system.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.DictDTO;
import com.ferry.system.api.dto.query.DictQuery;
import com.ferry.system.api.entity.Dict;
import com.ferry.system.api.vo.DictVO;
import com.ferry.system.api.vo.tree.DictTree;

import java.util.List;

public interface IDictService extends IService<Dict> {

    /**
     * 字典管理-树形列表
     *
     * @param body 查询条件
     * @return 树形列表
     */
    List<DictTree> treeList(DictQuery body);

    /**
     * 字典管理-分页列表
     *
     * @param body 查询条件
     * @return 分页列表
     */
    IPage<DictVO> pageList(DictQuery body);

    /**
     * 字典管理-详情信息
     *
     * @param id 字典id
     * @return 详情信息
     */
    DictTree detail(Long id);

    /**
     * 字典管理-编辑信息
     *
     * @param body 表单数据
     * @return 是否成功
     */
    boolean save(DictDTO body);

    /**
     * 字典管理-删除信息
     *
     * @param ids 字典id，支持批量
     * @return 是否成功
     */
    boolean deleted(List<Long> ids);
}
