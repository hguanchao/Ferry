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
 */
@Mapper
@DataScope(value = "dev_app", deptColumn = "dept_id")
public interface AppMapper extends BaseMapper<App> {

    /**
     * 统计同编码的应用数量，编码全局唯一，校验需忽略数据权限
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
     * <p>筛选条件写在 XML，数据权限由拦截器追加。
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 应用列表
     */
    IPage<AppVO> selectPageList(IPage<AppVO> page, @Param("query") AppQuery query);
}
