package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.security.annotation.DataScope;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.vo.PipelineVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流水线定义数据访问
 *
 * <p>流水线没有自己的团队字段，可见范围跟随所属应用：列表查询必须 INNER JOIN dev_app，
 * 数据权限条件会按 JOIN 里的别名生成（列表查询用 a.dept_id）；详情先按 id 取流水线，
 * 再用应用查询的数据权限确认可见性。
 *
 * <p>BaseMapper 的 getById / removeByIds 不会带 dev_app，编辑、删除与版本操作前必须先用
 * {@link #selectVisibleById} 校验可见性，不能直接按 id 改删。
 */
@Mapper
@DataScope(value = "dev_app", deptColumn = "dept_id")
public interface PipelineMapper extends BaseMapper<Pipeline> {

    /**
     * 分页查询流水线列表
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 流水线列表
     */
    IPage<PipelineVO> selectPageList(IPage<PipelineVO> page, @Param("query") PipelineQuery query);

    /**
     * 查询用于导出的流水线数据
     *
     * <p>与分页共用同一段查询条件；勾选导出也走这条语句，靠 ids 收窄结果集，
     * 不另写按 id 直查的 SQL——数据权限由 dev_app 注入，绕过它就会导出权限外的数据。
     *
     * @param query 查询条件
     * @param ids   勾选的流水线id，为空表示按查询条件导出
     * @param limit 最大导出条数
     * @return 流水线列表
     */
    List<PipelineVO> selectExportList(@Param("query") PipelineQuery query, @Param("ids") List<Long> ids,
                                      @Param("limit") int limit);

    /**
     * 按 id 查询可见流水线
     *
     * @param id 流水线id
     * @return 流水线，不存在或不可见时返回 null
     */
    Pipeline selectVisibleById(@Param("id") Long id);

    /**
     * 统计给定 id 中可见流水线的数量
     *
     * <p>删除前用它确认每个 id 都在当前用户的数据权限范围内，避免 BaseMapper 绕过数据权限。
     *
     * @param ids 流水线id集合
     * @return 可见记录数
     */
    long selectVisibleCount(@Param("ids") List<Long> ids);

    /**
     * 统计同一应用下的同名流水线数量
     *
     * @param appId 应用id
     * @param name  流水线名称
     * @param id    编辑时的流水线id，新增时传 null
     * @return 同名记录数
     */
    long selectNameCount(@Param("appId") Long appId, @Param("name") String name, @Param("id") Long id);
}
