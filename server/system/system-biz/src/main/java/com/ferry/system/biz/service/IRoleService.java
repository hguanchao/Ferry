package com.ferry.system.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.system.api.dto.RoleAuthorize;
import com.ferry.system.api.dto.RoleDTO;
import com.ferry.system.api.dto.query.RoleQuery;
import com.ferry.system.api.entity.Role;
import com.ferry.system.api.vo.RoleVO;
import com.ferry.system.api.vo.tree.MenuTree;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface IRoleService extends IService<Role> {

    /**
     * 角色管理-分页列表
     *
     * @param body 查询条件
     * @return 分页列表
     */
    IPage<RoleVO> pageList(RoleQuery body);

    /**
     * 角色管理-编辑信息
     *
     * @param body 表单数据
     * @return 是否成功
     */
    boolean save(RoleDTO body);

    /**
     * 角色管理-删除信息
     *
     * @param ids 数据id，支持批量
     * @return 是否成功
     */
    boolean deleted(List<Long> ids);

    /**
     * 角色管理-角色菜单列表
     *
     * @param id 角色id
     * @return 菜单列表
     */
    List<MenuTree> menuList(Long id);

    /**
     * 角色管理-角色授权菜单
     *
     * @param body 表单数据
     * @return 是否成功
     */
    boolean authorizeMenu(RoleAuthorize<Long, Long> body);

    /**
     * 角色管理-角色数据范围详情
     *
     * @param id 角色id
     * @return 数据范围与已授权的部门
     */
    RoleVO dataScope(Long id);

    /**
     * 角色管理-角色授权数据范围
     *
     * @param id        角色id
     * @param dataScope 数据范围
     * @param deptIds   自定义部门id集合
     * @return 是否成功
     */
    boolean authorizeDataScope(Long id, Integer dataScope, List<Long> deptIds);
}
