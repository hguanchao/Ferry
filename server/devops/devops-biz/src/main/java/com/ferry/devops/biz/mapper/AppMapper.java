package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.security.annotation.DataScope;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.vo.AppVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 应用档案数据访问
 *
 * <p>按所属团队过滤：可见范围取自登录会话快照，业务 SQL 不重复拼部门条件。
 */
@Mapper
@DataScope(value = "dev_app", deptColumn = "dept_id")
public interface AppMapper extends BaseMapper<App> {

    /**
     * 统计同编码的应用数量
     *
     * <p>编码是全局唯一（唯一索引不含 is_deleted），校验不能被当前用户的数据权限裁掉，
     * 否则看不见的同码应用会被判为可用，最后撞上唯一索引变成 500。
     *
     * @param code 应用编码
     * @param id   编辑时的应用id，新增时传 null
     * @return 同编码的记录数
     */
    @InterceptorIgnore(dataPermission = "1")
    long selectCodeCount(@Param("code") String code, @Param("id") Long id);

    /**
     * 分页查询应用列表
     *
     * <p>筛选条件写在 XML 里：负责人是多值逗号串、平台与团队是多选，用 SQL 表达比拼 Wrapper 直观。
     * 数据权限由拦截器按 dept_id 追加，这里不重复写可见范围条件。
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 应用列表
     */
    IPage<AppVO> selectPageList(IPage<AppVO> page, @Param("query") AppQuery query);
}
