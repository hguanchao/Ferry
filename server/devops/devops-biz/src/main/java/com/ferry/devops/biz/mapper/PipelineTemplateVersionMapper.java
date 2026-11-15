package com.ferry.devops.biz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ferry.devops.api.entity.PipelineTemplate;
import com.ferry.devops.api.entity.PipelineTemplateVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流水线模板版本数据访问
 */
@Mapper
public interface PipelineTemplateVersionMapper extends BaseMapper<PipelineTemplateVersion> {

    /**
     * 查询模板的当前版本指针信息
     *
     * @param templateId 模板id
     * @return 模板基本信息，不存在时返回 null
     */
    PipelineTemplate selectTemplateInfo(@Param("templateId") Long templateId);

    /**
     * 保存版本成功后回写模板当前版本指针与冗余计数
     *
     * @param templateId      模板id
     * @param currentVersionId 当前版本id
     * @param currentVersionNo 当前版本号
     * @param stageCount      阶段数
     * @param variableCount   占位变量数
     * @return 更新行数
     */
    int updateTemplate(@Param("templateId") Long templateId,
                       @Param("currentVersionId") Long currentVersionId,
                       @Param("currentVersionNo") Integer currentVersionNo,
                       @Param("stageCount") Integer stageCount,
                       @Param("variableCount") Integer variableCount);
}
