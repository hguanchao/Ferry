package com.ferry.system.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.system.api.entity.Menu;
import com.ferry.system.api.entity.RoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {

    /**
     * 查询角色授权的菜单列表
     * 注：支持批量查询，角色id按逗号拼接
     *
     * @param roleIds 角色id
     * @return List
     */
    List<Menu> selectRoleMenuListByRoleId(@Param("roleIds") String roleIds);
}