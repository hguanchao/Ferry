package com.ferry.system.biz.service;

import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.MenuDTO;
import com.ferry.system.api.dto.query.MenuQuery;
import com.ferry.system.api.entity.Menu;
import com.ferry.system.api.vo.tree.MenuTree;

import java.util.List;

public interface IMenuService extends IService<Menu> {

    /**
     * 菜单管理-树形列表
     *
     * @param body 查询条件
     * @return List<TreeNode> 树形列表
     */
    List<MenuTree> treeList(MenuQuery body);

    /**
     * 菜单管理-编辑信息
     *
     * @param body 表单数据
     * @return true 成功 || false 失败
     */
    boolean save(MenuDTO body);

    /**
     * 菜单管理-删除信息
     *
     * @param ids 菜单id(支持批量)
     * @return true 成功 || false 失败
     */
    boolean deleted(List<Long> ids);
}
