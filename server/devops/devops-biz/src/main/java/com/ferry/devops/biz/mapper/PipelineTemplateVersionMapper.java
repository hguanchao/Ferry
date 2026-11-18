package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.PipelineTemplateVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PipelineTemplateVersionMapper extends BaseMapper<PipelineTemplateVersion> {

    /**
     * 查询模板最大版本号
     */
    @Select("SELECT IFNULL(MAX(version_no), 0) FROM dev_pipeline_template_version WHERE template_id = #{templateId}")
    Integer selectMaxVersionNo(@Param("templateId") Long templateId);
}
