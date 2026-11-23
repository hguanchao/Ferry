package com.ferry.devops.biz.service.impl;

import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineStageTaskDTO;
import com.ferry.devops.api.entity.PipelineStage;
import com.ferry.devops.biz.mapper.PipelineStageMapper;
import com.ferry.devops.biz.service.IPipelineStageTaskService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 按流水线与版本从阶段/任务表装配结构化定义。
 *
 * <p>阶段服务与 Job 同步共用同一装配口径，避免两处实现漂移；
 * 通过 Mapper 读取阶段行而非依赖阶段服务，避免 Job 同步与阶段服务形成循环依赖。
 */
final class PipelineDefinitionLoader {

    private PipelineDefinitionLoader() {
    }

    /**
     * 读取指定版本的定义。
     *
     * @param stageMapper 阶段 Mapper
     * @param taskService 阶段任务服务
     * @param pipelineId  流水线 id
     * @param version     版本号
     * @return 结构化定义，阶段列表为空时返回空列表
     */
    static PipelineDefinitionDTO load(PipelineStageMapper stageMapper,
                                      IPipelineStageTaskService taskService,
                                      Long pipelineId, Integer version) {
        PipelineDefinitionDTO definition = new PipelineDefinitionDTO();
        List<PipelineStage> stages = stageMapper.selectByPipelineVersion(pipelineId, version);
        List<PipelineStageDTO> stageDtos = new ArrayList<>();
        if (!stages.isEmpty()) {
            List<Long> stageIds = stages.stream().map(PipelineStage::getId).toList();
            Map<Long, List<PipelineStageTaskDTO>> tasksByStage = taskService.mapTasksByStageIds(stageIds);
            for (PipelineStage stage : stages) {
                PipelineStageDTO stageDto = new PipelineStageDTO();
                stageDto.setType(stage.getType());
                stageDto.setName(stage.getName());
                stageDto.setBuildTool(stage.getBuildTool());
                stageDto.setTimeout(stage.getTimeout());
                stageDto.setOutputs(stage.getOutputs());
                stageDto.setTasks(tasksByStage.getOrDefault(stage.getId(), List.of()));
                stageDtos.add(stageDto);
            }
        }
        definition.setStages(stageDtos);
        return definition;
    }
}
