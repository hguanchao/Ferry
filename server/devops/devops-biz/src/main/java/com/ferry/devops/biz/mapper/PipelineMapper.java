package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.Pipeline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PipelineMapper extends BaseMapper<Pipeline> {

    /**
     * 流水线是否存在进行中的运行
     */
    @Select("SELECT COUNT(1) FROM dev_run WHERE is_deleted = 0 AND pipeline_id = #{pipelineId} AND status IN (1,2,3)")
    long countActiveRun(@Param("pipelineId") Long pipelineId);

    /**
     * 应用是否启用（触发预校验：流水线启用状态由主表读取，此方法保留给应用状态检查）
     */
    @Select("SELECT COUNT(1) FROM dev_pipeline WHERE is_deleted = 0 AND id = #{pipelineId} AND is_enabled = 1")
    long countEnabled(@Param("pipelineId") Long pipelineId);

    /**
     * 应用名称（列表回填）
     */
    @Select("SELECT name FROM dev_app WHERE is_deleted = 0 AND id = #{appId} LIMIT 1")
    String selectAppName(@Param("appId") Long appId);

    /**
     * 应用主仓库地址（触发预校验：仓库可达检查）
     */
    @Select("SELECT repo_url FROM dev_app_repo WHERE is_deleted = 0 AND app_id = #{appId} AND is_main = 1 LIMIT 1")
    String selectRepoUrl(@Param("appId") Long appId);

    /**
     * 应用默认分支（触发预校验：分支存在检查）
     */
    @Select("SELECT default_branch FROM dev_app_repo WHERE is_deleted = 0 AND app_id = #{appId} AND is_main = 1 LIMIT 1")
    String selectDefaultBranch(@Param("appId") Long appId);
}
