package com.ferry.devops.biz.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ferry.common.core.utils.JsonUtil;
import com.ferry.common.jdbc.base.ServiceImpl;
import com.ferry.devops.api.dto.PipelineTaskDTO;
import com.ferry.devops.api.entity.PipelineStageTask;
import com.ferry.devops.biz.mapper.PipelineStageTaskMapper;
import com.ferry.devops.biz.service.IPipelineStageTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 流水线阶段任务服务实现。
 *
 * <p>executor 在传输层是字符串（jenkins/deployer/gate/release），落库为数字码 1/2/3/4。
 */
@Slf4j
@Service
public class PipelineStageTaskServiceImpl extends ServiceImpl<PipelineStageTaskMapper, PipelineStageTask>
        implements IPipelineStageTaskService {

    private static final Map<String, Integer> EXECUTOR_CODE = Map.of(
            "jenkins", 1, "deployer", 2, "gate", 3, "release", 4);
    private static final Map<Integer, String> EXECUTOR_NAME = Map.of(
            1, "jenkins", 2, "deployer", 3, "gate", 4, "release");

    @Override
    public void saveTasks(Long stageId, Integer version, List<PipelineTaskDTO> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        List<PipelineStageTask> entities = new ArrayList<>();
        int sort = 1;
        for (PipelineTaskDTO task : tasks) {
            PipelineStageTask entity = new PipelineStageTask();
            entity.setStageId(stageId);
            entity.setVersion(version);
            entity.setName(task.getName());
            entity.setExecutor(EXECUTOR_CODE.getOrDefault(task.getExecutor(), 1));
            entity.setScriptRef(task.getScriptRef());
            entity.setScriptContent(task.getScriptContent());
            entity.setScriptParams(task.getParams() == null ? null : JsonUtil.toJson(task.getParams()));
            entity.setSort(sort++);
            entities.add(entity);
        }
        saveBatch(entities);
    }

    @Override
    public Map<Long, List<PipelineTaskDTO>> mapTasksByStageIds(List<Long> stageIds) {
        if (stageIds == null || stageIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<PipelineTaskDTO>> result = new HashMap<>();
        for (PipelineStageTask task : baseMapper.selectByStageIds(stageIds)) {
            result.computeIfAbsent(task.getStageId(), key -> new ArrayList<>()).add(toTaskDto(task));
        }
        return result;
    }

    @Override
    public void deleteByStageIds(List<Long> stageIds) {
        if (stageIds == null || stageIds.isEmpty()) {
            return;
        }
        remove(Wrappers.<PipelineStageTask>lambdaQuery()
                .in(PipelineStageTask::getStageId, stageIds));
    }

    private PipelineTaskDTO toTaskDto(PipelineStageTask task) {
        PipelineTaskDTO dto = new PipelineTaskDTO();
        dto.setName(task.getName());
        dto.setExecutor(EXECUTOR_NAME.getOrDefault(task.getExecutor(), "jenkins"));
        dto.setScriptRef(task.getScriptRef());
        dto.setScriptContent(task.getScriptContent());
        if (task.getScriptParams() != null && !task.getScriptParams().isBlank()) {
            try {
                dto.setParams(JsonUtil.toBean(task.getScriptParams(), Map.class));
            } catch (Exception e) {
                log.warn("流水线任务脚本参数解析失败: taskId={}", task.getId(), e);
                dto.setParams(null);
            }
        }
        return dto;
    }
}