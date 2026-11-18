package com.ferry.devops.biz.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ferry.common.jdbc.base.IService;
import com.ferry.devops.api.dto.PipelineDTO;
import com.ferry.devops.api.dto.PipelineTemplateVersionDTO;
import com.ferry.devops.api.dto.query.PipelineQuery;
import com.ferry.devops.api.entity.Pipeline;
import com.ferry.devops.api.vo.DiffVO;
import com.ferry.devops.api.vo.PipelineDefinitionVO;
import com.ferry.devops.api.vo.PipelineDetailVO;
import com.ferry.devops.api.vo.PipelineVO;
import com.ferry.devops.api.vo.TriggerRecordVO;
import com.ferry.devops.api.vo.ValidateResultVO;
import com.ferry.devops.api.vo.VersionVO;

import java.util.List;

/**
 * 流水线服务
 */
public interface IPipelineService extends IService<Pipeline> {

    IPage<PipelineVO> pageList(PipelineQuery body);

    PipelineDetailVO detail(Long id);

    boolean save(PipelineDTO body);

    boolean deleted(List<Long> ids);

    boolean enable(Long id);

    boolean disable(Long id, String reason);

    PipelineDefinitionVO definition(Long id, Long versionId);

    ValidateResultVO validateDefinition(String definition, Integer mode);

    VersionVO saveDefinition(Long id, String definition, Integer mode, String changeNote);

    IPage<VersionVO> versionPage(Long pipelineId, Long current, Long size);

    DiffVO versionDiff(Long leftId, Long rightId);

    VersionVO rollback(Long id, Long versionId, String changeNote);

    IPage<TriggerRecordVO> triggerRecords(Long appId, Long current, Long size);
}
