package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.devops.api.dto.query.AppQuery;
import com.ferry.devops.api.entity.App;
import com.ferry.devops.api.vo.AppVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppMapper extends BaseMapper<App> {

    IPage<AppVO> selectPageList(IPage<App> page, @Param("query") AppQuery query);

    /**
     * 应用是否存在进行中的运行
     */
    @Select("SELECT COUNT(1) FROM dev_run WHERE is_deleted = 0 AND app_id = #{appId} AND status IN (1,2,3,8)")
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
