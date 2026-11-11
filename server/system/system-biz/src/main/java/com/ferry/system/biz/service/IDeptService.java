package com.ferry.system.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.DeptDTO;
import com.ferry.system.api.dto.query.DeptQuery;
import com.ferry.system.api.entity.Dept;
import com.ferry.system.api.vo.tree.DeptTree;

import java.util.List;

public interface IDeptService extends IService<Dept> {

    /**
     * 部门管理-分页列表
     *
     * @param body 查询条件
     * @return IPage<DeptTree> 分页列表
     */
    IPage<DeptTree> pageList(DeptQuery body);

    /**
     * 部门管理-树结构列表
     *
     * @param body 查询条件
     * @return List<DeptTree> 树结构列表
     */
    List<DeptTree> treeList(DeptQuery body);

    /**
     * 部门管理-详情信息
     *
     * @param id 部门id
     * @return DeptTree 详情信息
     */
    DeptTree detail(Long id);

    /**
     * 部门管理-编辑信息
     *
     * @param body 表单数据
     * @return true 修改成功 || false 修改失败
     */
    boolean save(DeptDTO body);

    /**
     * 部门管理-删除信息
     *
     * @param ids 部门id(支持批量)
     * @return true 删除成功 || false 删除失败
     */
    boolean deleted(List<Long> ids);

    /**
     * 查询部门及各自全部下级部门id
     *
     * <p>单个部门传 List.of(id) 即可，与「本部门及以下」「自定义范围」共用同一条展开规则。
     *
     * @param deptIds 部门id集合
     * @return 部门id集合，入参为空时返回空集合
     */
    List<Long> selfAndChildIds(List<Long> deptIds);
}
