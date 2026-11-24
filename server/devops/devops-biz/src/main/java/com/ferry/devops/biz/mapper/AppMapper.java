package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.security.annotation.DataScope;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.vo.AppVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 应用档案 Mapper。
 *
 * <p>标注数据权限：dev_app 按 dept_id（团队）与 owner_id（负责人）过滤，
 * 超管与 dataAll 用户由 DataPermissionHandler 自动放行。列表、选项、编码查重都走同一范围。
 */
@Mapper
@DataScope(value = "dev_app", deptColumn = "dept_id", userColumn = "owner_id")
public interface AppMapper extends BaseMapper<App> {

    IPage<AppVO> selectPageList(IPage<App> page, @Param("query") AppQuery query);

    /**
     * 应用下未删除的流水线数量；删除应用前要求先清理流水线，避免留下孤儿数据。
     */
    @Select("SELECT COUNT(1) FROM dev_pipeline WHERE is_deleted = 0 AND app_id = #{appId}")
    long countPipeline(@Param("appId") Long appId);

    /**
     * 应用是否存在进行中的运行
     */
    @Select("SELECT COUNT(1) FROM dev_run WHERE is_deleted = 0 AND app_id = #{appId} AND status IN (0,1,2,3)")
    long countActiveRun(@Param("appId") Long appId);

    /**
     * 应用是否存在进行中的部署
     */
    @Select("SELECT COUNT(1) FROM dev_deploy_record WHERE is_deleted = 0 AND app_id = #{appId} AND status IN (1,2,3)")
    long countActiveDeploy(@Param("appId") Long appId);

    /**
     * 应用是否存在未关闭的发布单
     */
    @Select("SELECT COUNT(1) FROM dev_release_artifact ra JOIN dev_release_candidate rc ON ra.rc_id = rc.id "
            + "WHERE rc.is_deleted = 0 AND rc.status IN (2,3,4,5) AND ra.app_id = #{appId}")
    long countOpenRelease(@Param("appId") Long appId);
}
