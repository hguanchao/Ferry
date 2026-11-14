package com.ferry.system.biz.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.common.security.annotation.DataScope;
import com.ferry.system.api.entity.Dept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门数据访问
 */
@Mapper
@DataScope(value = "sys_dept", deptColumn = "id")
public interface DeptMapper extends BaseMapper<Dept> {

    /**
     * 统计关联这些部门的用户数量
     *
     * @param ids 部门id集合
     * @return 用户数量
     */
    long selectUserCountByIds(@Param("ids") List<Long> ids);

    /**
     * 查询部门及各自全部下级部门id，一次算完避免逐个回表
     *
     * <p>登录时还没有租户上下文，且本语句算的就是可见范围，因此跳过租户与数据权限拦截。
     *
     * @param ids 部门id集合
     * @return 部门id集合
     */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "1")
    List<Long> selectSelfAndChildIdsByDeptIds(@Param("ids") List<Long> ids);
}
