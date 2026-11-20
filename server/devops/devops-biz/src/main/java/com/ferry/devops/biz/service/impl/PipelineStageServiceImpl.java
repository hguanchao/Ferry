package com.ferry.devops.biz.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineTaskDTO;
import com.ferry.devops.api.entity.PipelineStage;
import com.ferry.devops.biz.mapper.PipelineStageMapper;
import com.ferry.devops.biz.service.IPipelineStageService;
import com.ferry.devops.biz.service.IPipelineStageTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流水线阶段服务实现：管理阶段记录，并通过阶段任务服务装配任务。
 */
@Service
@RequiredArgsConstructor
public class PipelineStageServiceImpl extends ServiceImpl<PipelineStageMapper, PipelineStage>
        implements IPipelineStageService {

    private final IPipelineStageTaskService pipelineStageTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDefinition(Long pipelineId, Integer version, PipelineDefinitionDTO definition) {
        List<PipelineStageDTO> stages = definition == null ? null : definition.getStages();
        if (stages == null) {
            return;
        }
        int sort = 1;
        for (PipelineStageDTO stage : stages) {
            PipelineStage entity = new PipelineStage();
            entity.setPipelineId(pipelineId);
            entity.setVersion(version);
            entity.setType(stage.getType());
            entity.setName(stage.getName());
            entity.setBuildTool(stage.getBuildTool());
            entity.setTimeout(stage.getTimeoutSeconds());
            entity.setOutputs(stage.getOutputs() == null ? null : String.join(",", stage.getOutputs()));
            entity.setSort(sort++);
            save(entity);
            pipelineStageTaskService.saveTasks(entity.getId(), version, stage.getTasks());
        }
    }

    @Override
    public PipelineDefinitionDTO loadDefinition(Long pipelineId, Integer version) {
        PipelineDefinitionDTO definition = new PipelineDefinitionDTO();
        List<PipelineStage> stages = baseMapper.selectByPipelineVersion(pipelineId, version);
        List<PipelineStageDTO> stageDtos = new ArrayList<>();
        if (!stages.isEmpty()) {
            List<Long> stageIds = stages.stream().map(PipelineStage::getId).toList();
            Map<Long, List<PipelineTaskDTO>> tasksByStage =
                    pipelineStageTaskService.mapTasksByStageIds(stageIds);
            for (PipelineStage stage : stages) {
                PipelineStageDTO stageDto = new PipelineStageDTO();
                stageDto.setType(stage.getType());
                stageDto.setName(stage.getName());
                stageDto.setBuildTool(stage.getBuildTool());
                stageDto.setTimeoutSeconds(stage.getTimeout());
                if (stage.getOutputs() != null && !stage.getOutputs().isBlank()) {
                    stageDto.setOutputs(List.of(stage.getOutputs().split(",")));
                } else {
                    stageDto.setOutputs(new ArrayList<>());
                }
                stageDto.setTasks(tasksByStage.getOrDefault(stage.getId(), List.of()));
                stageDtos.add(stageDto);
            }
        }
        definition.setStages(stageDtos);
        return definition;
    }

    @Override
    public String loadDefinitionJson(Long pipelineId, Integer version) {
        return JsonUtil.toJson(loadDefinition(pipelineId, version));
    }

    @Override
    public List<Integer> listVersions(Long pipelineId) {
        return baseMapper.selectDistinctVersions(pipelineId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByPipeline(Long pipelineId) {
        List<PipelineStage> stages = list(Wrappers.<PipelineStage>lambdaQuery()
                .eq(PipelineStage::getPipelineId, pipelineId));
        if (stages.isEmpty()) {
            return;
        }
        List<Long> stageIds = stages.stream().map(PipelineStage::getId).collect(Collectors.toList());
        pipelineStageTaskService.deleteByStageIds(stageIds);
        removeByIds(stageIds);
    }
}
