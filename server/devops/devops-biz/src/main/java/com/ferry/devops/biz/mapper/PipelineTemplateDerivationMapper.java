package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.PipelineTemplateDerivation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PipelineTemplateDerivationMapper extends BaseMapper<PipelineTemplateDerivation> {

    /**
     * 模板的派生流水线数
     */
    @Select("SELECT COUNT(1) FROM dev_pipeline_template_derivation WHERE template_id = #{templateId}")
    long countByTemplate(@Param("templateId") Long templateId);

    /**
     * 标记模板的所有派生流水线为落后
     */
    @Update("UPDATE dev_pipeline_template_derivation SET is_behind = 1 WHERE template_id = #{templateId}")
    int markBehindByTemplate(@Param("templateId") Long templateId);

    /**
     * 按流水线查派生关系
     */
    @Select("SELECT * FROM dev_pipeline_template_derivation WHERE pipeline_id = #{pipelineId} LIMIT 1")
    PipelineTemplateDerivation selectByPipeline(@Param("pipelineId") Long pipelineId);
}
