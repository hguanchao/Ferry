package com.ferry.system.biz.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.system.api.entity.Dept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
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
     *
     * @param deptId 部门id
     * @return 部门id集合
     */
    @InterceptorIgnore(tenantLine = "true")
    List<Long> selectSelfAndChildIds(@Param("deptId") Long deptId);

    /**
     * 把子孙路径里的旧前缀换成新前缀
     *
     * @param deptId    被移动的部门id
     * @param oldPrefix 移动前该部门在路径中的前缀
     * @param newPrefix 移动后该部门在路径中的前缀
     * @return int 更新行数
     */
    int updateChildrenAncestors(@Param("deptId") Long deptId,
                                @Param("oldPrefix") String oldPrefix,
                                @Param("newPrefix") String newPrefix);
}
