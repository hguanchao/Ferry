package com.ferry.system.biz.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.system.api.entity.Dept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

    long selectUserCountById(@Param("id") Long id);

    /**
     * 查询本部门及全部下级部门id
     *
     * <p>登录时还没有租户上下文，跳过拦截器；入参部门id本身已经限定了范围。
     *
     * @param deptId 部门id
     * @return 部门id集合
     */
    @InterceptorIgnore(tenantLine = "true")
    List<Long> selectSelfAndChildIds(@Param("deptId") Long deptId);
}
