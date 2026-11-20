package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.vo.PipelineVO;
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
    @Select("SELECT COUNT(1) FROM dev_pipeline WHERE is_deleted = 0 AND id = #{pipelineId} AND status = 1")
    long countEnabled(@Param("pipelineId") Long pipelineId);

    /**
     * 应用名称（列表回填）
     */
    @Select("SELECT name FROM dev_app WHERE is_deleted = 0 AND id = #{appId} LIMIT 1")
    String selectAppName(@Param("appId") Long appId);

    /**
     * 应用默认分支（触发预校验：分支存在检查）
     * TODO(dev_app-schema): dev_app_repo 已删除，分支字段改为 dev_app.repo_branch。
     */
    @Select("SELECT repo_branch FROM dev_app WHERE is_deleted = 0 AND id = #{appId} LIMIT 1")
    String selectDefaultBranch(@Param("appId") Long appId);

    /**
     * 分页查询流水线列表（XML 联表：应用/负责人/最近运行摘要）。
     *
     * @param page  分页参数
     * @param query 筛选条件
     * @return 流水线列表项分页结果
     */
    IPage<PipelineVO> selectPageList(IPage<PipelineVO> page, @Param("query") PipelineQuery query);
}
