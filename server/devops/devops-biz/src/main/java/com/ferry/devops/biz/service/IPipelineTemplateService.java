package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.PipelineTemplateDTO;
import com.ferry.devops.api.dto.PipelineTemplateVersionDTO;
import com.ferry.devops.api.dto.query.TemplateQuery;
import com.ferry.devops.api.entity.PipelineTemplate;
import com.ferry.devops.api.vo.DerivationVO;
import com.ferry.devops.api.vo.DiffVO;
import com.ferry.devops.api.vo.PipelineTemplateVO;
import com.ferry.devops.api.vo.PipelineTemplateVersionVO;
import com.ferry.devops.api.vo.TemplateVariableVO;
import com.ferry.devops.api.vo.VersionVO;

import java.util.List;

/**
 * 流水线模板服务
 */
public interface IPipelineTemplateService extends IService<PipelineTemplate> {

    IPage<PipelineTemplateVO> pageList(TemplateQuery body);

    PipelineTemplateVersionVO detail(Long id);

    boolean save(PipelineTemplateDTO body);

    boolean deleted(List<Long> ids);

    boolean enable(Long id);

    boolean disable(Long id, String reason);

    IPage<VersionVO> versionPage(Long templateId, Long current, Long size);

    DiffVO versionDiff(Long leftId, Long rightId);

    VersionVO rollback(PipelineTemplateVersionDTO body);

    List<DerivationVO> derivations(Long id);

    VersionVO merge(PipelineTemplateVersionDTO body);

    List<TemplateVariableVO> variables(Long versionId);
}
