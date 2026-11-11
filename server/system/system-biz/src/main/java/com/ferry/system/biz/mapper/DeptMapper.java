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
 *
 * <p>部门表的主键就叫 id，没有独立的部门字段，所以把 deptColumn 指到 id 上。
 */
@Mapper
@DataScope(value = "sys_dept", deptColumn = "id")
public interface DeptMapper extends BaseMapper<Dept> {

    /**
     * 统计关联这些部门的用户数量
     *
     * @param ids 部门id集合
     * @return long 用户数量
     */
    long selectUserCountByIds(@Param("ids") List<Long> ids);

    /**
     * 查询本部门及全部下级部门id
     *
     * <p>登录时还没有租户上下文，跳过拦截器；入参部门id本身已经限定了范围。
     * 数据权限也必须跳过：这条语句算的就是可见范围，再按范围过滤等于拿结果去筛它的来源。
     *
     * @param id 部门id
     * @return 部门id集合
     */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "1")
    List<Long> selectSelfAndChildIds(@Param("id") Long id);

    /**
     * 查询多个部门及各自全部下级部门id
     *
     * <p>一次算完某个角色勾选的全部部门，避免按部门逐个回表；跳过拦截器的原因同上。
     *
     * @param ids 部门id集合
     * @return 部门id集合
     */
    @InterceptorIgnore(tenantLine = "true", dataPermission = "1")
    List<Long> selectSelfAndChildIdsByDeptIds(@Param("ids") List<Long> ids);
}
