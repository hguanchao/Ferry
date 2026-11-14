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
     *
     * @param roleIds 角色id，多个按逗号拼接
     * @return 菜单列表
     */
    List<Menu> selectRoleMenuListByRoleId(@Param("roleIds") String roleIds);
}