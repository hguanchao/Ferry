package com.ferry.devops.biz.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.devops.api.dto.PipelineDefinitionDTO;
import com.ferry.devops.api.dto.PipelineStageDTO;
import com.ferry.devops.api.dto.PipelineTaskDTO;
import com.ferry.devops.api.entity.PipelineStage;
import com.ferry.devops.api.entity.PipelineStageTask;
import com.ferry.devops.biz.mapper.PipelineStageMapper;
import com.ferry.devops.biz.mapper.PipelineStageTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 流水线定义存储：把结构化定义（JSON DTO）落库到阶段表/任务表，并按版本读回。
 *
 * <p>对上层仍以 {@link PipelineDefinitionDTO} 交互，JSON 仅作为 API 传输格式，
 * 持久化采用 dev_pipeline_stage + dev_pipeline_stage_task 关系表。
 * executor 在传输层是字符串（jenkins/deployer/gate/release），落库为数字码 1/2/3/4。
 */
@Component
@RequiredArgsConstructor
public class PipelineDefinitionStore {

    private final PipelineStageMapper stageMapper;
    private final PipelineStageTaskMapper taskMapper;

    private static final Map<String, Integer> EXECUTOR_CODE = Map.of(
            "jenkins", 1, "deployer", 2, "gate", 3, "release", 4);
    private static final Map<Integer, String> EXECUTOR_NAME = Map.of(
            1, "jenkins", 2, "deployer", 3, "gate", 4, "release");

    /**
     * 保存一个版本的定义：写入阶段与任务行。调用方需保证 version 是流水线的新版本号。
     */
    public void save(Long pipelineId, Integer version, PipelineDefinitionDTO definition) {
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
            entity.setTimeout(stage.getTimeoutSeconds());
            entity.setOutputs(stage.getOutputs() == null ? null : String.join(",", stage.getOutputs()));
            entity.setSort(sort++);
            stageMapper.insert(entity);

            int taskSort = 1;
            if (stage.getTasks() != null) {
                for (PipelineTaskDTO task : stage.getTasks()) {
                    PipelineStageTask taskEntity = new PipelineStageTask();
                    taskEntity.setStageId(entity.getId());
                    taskEntity.setVersion(version);
                    taskEntity.setName(task.getName());
                    taskEntity.setExecutor(EXECUTOR_CODE.getOrDefault(task.getExecutor(), 1));
                    taskEntity.setScriptRef(task.getScriptRef());
                    taskEntity.setScriptContent(task.getScriptContent());
                    taskEntity.setScriptParams(task.getParams() == null ? null : JsonUtil.toJson(task.getParams()));
                    taskEntity.setSort(taskSort++);
                    taskMapper.insert(taskEntity);
                }
            }
        }
    }

    /**
     * 读取一个版本的定义：从阶段/任务表组装回 DTO。
     */
    public PipelineDefinitionDTO load(Long pipelineId, Integer version) {
        PipelineDefinitionDTO definition = new PipelineDefinitionDTO();
        List<PipelineStage> stages = stageMapper.selectByPipelineVersion(pipelineId, version);
        List<PipelineStageDTO> result = new ArrayList<>();
        if (!stages.isEmpty()) {
            List<Long> stageIds = stages.stream().map(PipelineStage::getId).toList();
            Map<Long, List<PipelineStageTask>> tasksByStage = new HashMap<>();
            for (PipelineStageTask task : taskMapper.selectByStageIds(stageIds)) {
                tasksByStage.computeIfAbsent(task.getStageId(), k -> new ArrayList<>()).add(task);
            }
            for (PipelineStage stage : stages) {
                PipelineStageDTO dto = new PipelineStageDTO();
                dto.setType(stage.getType());
                dto.setName(stage.getName());
                dto.setTimeoutSeconds(stage.getTimeout());
                if (stage.getOutputs() != null && !stage.getOutputs().isBlank()) {
                    dto.setOutputs(List.of(stage.getOutputs().split(",")));
                } else {
                    dto.setOutputs(new ArrayList<>());
                }
                List<PipelineTaskDTO> tasks = new ArrayList<>();
                for (PipelineStageTask t : tasksByStage.getOrDefault(stage.getId(), List.of())) {
                    PipelineTaskDTO taskDto = new PipelineTaskDTO();
                    taskDto.setName(t.getName());
                    taskDto.setExecutor(EXECUTOR_NAME.getOrDefault(t.getExecutor(), "jenkins"));
                    taskDto.setScriptRef(t.getScriptRef());
                    taskDto.setScriptContent(t.getScriptContent());
                    if (t.getScriptParams() != null && !t.getScriptParams().isBlank()) {
                        try {
                            taskDto.setParams(JsonUtil.toBean(t.getScriptParams(), Map.class));
                        } catch (Exception ignored) {
                            taskDto.setParams(null);
                        }
                    }
                    tasks.add(taskDto);
                }
                dto.setTasks(tasks);
                result.add(dto);
            }
        }
        definition.setStages(result);
        return definition;
    }

    /**
     * 生成对应版本的 JSON 文本（API 传输格式）。
     */
    public String loadJson(Long pipelineId, Integer version) {
        return JsonUtil.toJson(load(pipelineId, version));
    }

    /**
     * 查询流水线已有的版本号列表（降序）。
     */
    public List<Integer> listVersions(Long pipelineId) {
        return stageMapper.selectDistinctVersions(pipelineId);
    }

    /**
     * 删除某流水线全部版本的定义行（删除流水线时调用）。
     */
    public void deleteByPipeline(Long pipelineId) {
        List<PipelineStage> stages = stageMapper.selectList(
                Wrappers.<PipelineStage>lambdaQuery().eq(PipelineStage::getPipelineId, pipelineId));
        if (!stages.isEmpty()) {
            taskMapper.delete(Wrappers.<PipelineStageTask>lambdaQuery()
                    .in(PipelineStageTask::getStageId, stages.stream().map(PipelineStage::getId).toList()));
        }
        stageMapper.delete(Wrappers.<PipelineStage>lambdaQuery().eq(PipelineStage::getPipelineId, pipelineId));
    }
}
